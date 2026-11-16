package CyberLadies.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;

import CyberLadies.CyberladiesApplication;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Bancos descartáveis, previamente criados e vazios. Nunca usa o banco padrão da aplicação.
@EnabledIfEnvironmentVariable(named="MIGRATION_TESTS", matches="true")
@EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class MigracaoPostgresTests {
    private static final String VAZIO = "cyberladies_migracao_vazia";
    private static final String LEGADO = "cyberladies_migracao_legado";
    private static final String NA_V1 = "cyberladies_migracao_v1";
    private static final String NA_V2 = "cyberladies_migracao_v2";
    private static final String AUTH = "Bearer " + "a".repeat(64);
    private static final String HASH_TOKEN = "ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb";
    private static final List<String> TABELAS = List.of("talk_usuaria", "talk_sessao", "talk_post",
        "talk_comentario", "talk_curtida", "talk_anexo");

    @Test
    void bancoVazioMigraUmaVezEReiniciaSemModificarDados() throws Exception {
        assertVazio(VAZIO);
        assertThrows(IllegalStateException.class, () -> adotar(VAZIO, "--adotar=" + VAZIO));
        try (var contexto = iniciar(VAZIO); var conexao = conectar(VAZIO)) {
            assertEquals("SQL", historico(conexao).getFirst().get(0));
            assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), historico(conexao).stream().map(linha -> linha.get(1)).toList());
            inserirDados(conexao, true);
            conferirAplicacao(contexto);
        }
        var antes = estado(VAZIO);
        try (var contexto = iniciar(VAZIO)) { assertNotNull(contexto.getBean(CyberladiesApplication.class)); }
        assertEquals(antes, estado(VAZIO));
    }

    @Test
    void adotaSchemaAnteriorPreservaDadosERecusaDivergencias() throws Exception {
        assertVazio(LEGADO);
        try (var conexao = conectar(LEGADO)) {
            ScriptUtils.executeSqlScript(conexao, new ClassPathResource("db/schema-anterior.sql"));
            inserirDados(conexao, false);
        }
        var antes = dados(LEGADO);
        assertThrows(FlywayException.class, () -> AdotarBanco.flyway(url(LEGADO), usuario(), senha()).migrate());
        assertSemHistorico(LEGADO);
        assertThrows(IllegalArgumentException.class, () -> adotar(LEGADO, "--adotar=outro_banco"));
        // Um schema incompleto ou alterado nunca pode receber baseline.
        executar(LEGADO, "DROP INDEX talk_post_autora_idx");
        assertThrows(IllegalStateException.class, () -> adotar(LEGADO, "--adotar=" + LEGADO));
        assertSemHistorico(LEGADO);
        executar(LEGADO, "CREATE INDEX talk_post_autora_idx ON talk_post(autora_id)");
        executar(LEGADO, "ALTER TABLE talk_usuaria ALTER COLUMN email DROP NOT NULL");
        assertThrows(IllegalStateException.class, () -> adotar(LEGADO, "--adotar=" + LEGADO));
        assertSemHistorico(LEGADO);
        executar(LEGADO, "ALTER TABLE talk_usuaria ALTER COLUMN email SET NOT NULL");
        executar(LEGADO, "ALTER TABLE talk_sessao DROP CONSTRAINT talk_sessao_usuaria_id_fkey");
        assertThrows(IllegalStateException.class, () -> adotar(LEGADO, "--adotar=" + LEGADO));
        assertSemHistorico(LEGADO);
        executar(LEGADO, "ALTER TABLE talk_sessao ADD CONSTRAINT talk_sessao_usuaria_id_fkey FOREIGN KEY (usuaria_id) REFERENCES talk_usuaria(id) ON DELETE CASCADE");
        adotar(LEGADO, "--verificar");
        assertSemHistorico(LEGADO);
        assertEquals(antes, dados(LEGADO));
        adotar(LEGADO, "--adotar=" + LEGADO);
        assertEquals(antes, dados(LEGADO));
        assertThrows(IllegalStateException.class, () -> adotar(LEGADO, "--adotar=" + LEGADO));
        try (var conexao = conectar(LEGADO)) {
            assertEquals("BASELINE", historico(conexao).getFirst().get(0));
        }
        var historicoV1 = estado(LEGADO).getLast();
        try (var contexto = iniciar(LEGADO)) { assertNotNull(contexto.getBean(CyberladiesApplication.class)); }
        assertEquals(antes, dados(LEGADO));
        try (var c = conectar(LEGADO)) {
            assertEquals(historicoV1, historico(c).getFirst());
            assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), historico(c).stream().map(linha -> linha.get(1)).toList());
            conferirEstruturaV2(c);
        }
        try (var contexto = iniciar(LEGADO)) { conferirAplicacao(contexto); }
        var depoisUso = estado(LEGADO);
        try (var contexto = iniciar(LEGADO)) { assertNotNull(contexto.getBean(CyberladiesApplication.class)); }
        assertEquals(depoisUso, estado(LEGADO));
    }

    @Test
    void atualizaBancoVersionadoNaV1PreservaTudoEReiniciaSemReaplicar() throws Exception {
        assertVazio(NA_V1);
        Flyway.configure().dataSource(url(NA_V1), usuario(), senha()).defaultSchema("public").schemas("public")
            .locations("classpath:db/migration").target("1").cleanDisabled(true).load().migrate();
        try (var c = conectar(NA_V1)) { inserirDados(c, false); }
        var antes = dados(NA_V1);
        List<List<String>> historicoAntes;
        try (var c = conectar(NA_V1)) { historicoAntes = historico(c); }
        try (var contexto = iniciar(NA_V1); var c = conectar(NA_V1)) {
            assertEquals(antes, dados(NA_V1));
            assertEquals(historicoAntes.getFirst(), historico(c).getFirst());
            assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), historico(c).stream().map(linha -> linha.get(1)).toList());
            conferirEstruturaV2(c);
            conferirAplicacao(contexto);
            assertEquals("5", consultar(c, "SELECT id::text FROM conta WHERE email='bia@example.com'").getFirst().getFirst());
        }
        var antesReinicio = estado(NA_V1);
        try (var contexto = iniciar(NA_V1)) { assertNotNull(contexto.getBean(CyberladiesApplication.class)); }
        assertEquals(antesReinicio, estado(NA_V1));
    }

    private void conferirEstruturaV2(Connection c) throws Exception {
        assertNull(consultar(c, "SELECT to_regclass('public.talk_usuaria')::text").getFirst().getFirst());
        assertNull(consultar(c, "SELECT to_regclass('public.talk_sessao')::text").getFirst().getFirst());
        assertEquals("0", consultar(c, "SELECT count(*)::text FROM information_schema.columns WHERE table_schema='public' AND table_name='conta' AND column_name IN ('membro','competencias')").getFirst().getFirst());
        assertEquals(List.of(List.of("1")), consultar(c, "SELECT conta_id::text FROM talk_participacao ORDER BY conta_id"));
        assertEquals("3", consultar(c, "SELECT count(*)::text FROM usuaria").getFirst().getFirst());
        assertEquals(List.of(List.of("1", "USUARIA"), List.of("2", "USUARIA"), List.of("3", "USUARIA")),
            consultar(c, "SELECT conta_id::text,papel FROM conta_papel ORDER BY conta_id,papel"));
        assertEquals("17", consultar(c, "SELECT count(*)::text FROM pg_constraint WHERE contype='f' AND confrelid='public.conta'::regclass").getFirst().getFirst());
        assertThrows(java.sql.SQLException.class, () -> {
            try (var s = c.createStatement()) { s.executeUpdate("INSERT INTO talk_participacao VALUES (999999)"); }
        });
    }

    @Test
    void atualizaV2AtribuindoSomenteUsuariaEMantendoSessoes() throws Exception {
        assertVazio(NA_V2);
        Flyway.configure().dataSource(url(NA_V2), usuario(), senha()).defaultSchema("public").schemas("public")
            .locations("classpath:db/migration").target("2").cleanDisabled(true).load().migrate();
        try (var c = conectar(NA_V2)) { inserirDados(c, true); }
        var antes = dados(NA_V2);
        try (var contexto = iniciar(NA_V2); var c = conectar(NA_V2)) {
            assertEquals(antes, dados(NA_V2));
            conferirEstruturaV2(c);
            var mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) contexto).build();
            mvc.perform(get("/api/auth/me").header("Authorization", AUTH)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.papeis[0]").value("USUARIA"))
                .andExpect(jsonPath("$.papeis.length()").value(1));
        }
        var estado = estado(NA_V2);
        try (var contexto = iniciar(NA_V2)) { assertNotNull(contexto.getBean(CyberladiesApplication.class)); }
        assertEquals(estado, estado(NA_V2));
    }

    private void conferirAplicacao(ConfigurableApplicationContext contexto) throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) contexto).build();
        mvc.perform(get("/api/usuaria").header("Authorization", AUTH))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Ana"));
        String authLia = "Bearer " + "b".repeat(64);
        mvc.perform(get("/api/usuaria").header("Authorization", authLia))
            .andExpect(status().isOk()).andExpect(jsonPath("$.competencias").value(""))
            .andExpect(jsonPath("$.membro").value(false));
        mvc.perform(get("/api/posts").header("Authorization", authLia)).andExpect(status().isForbidden());
        // Mesmo após aderir, a segunda conta não ganha autoria da publicação de Ana.
        mvc.perform(put("/api/comunidade/adesao").header("Authorization", authLia)).andExpect(status().isNoContent());
        mvc.perform(put("/api/comunidade/adesao").header("Authorization", authLia)).andExpect(status().isNoContent());
        mvc.perform(put("/api/posts/1").header("Authorization", authLia).contentType(MediaType.APPLICATION_JSON)
            .content("{\"texto\":\"Outra autora\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("Apenas a autora pode alterar a publicação."));
        mvc.perform(get("/api/posts").header("Authorization", AUTH))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].autora").value("Ana"))
            .andExpect(jsonPath("$[0].curtidas").value(1)).andExpect(jsonPath("$[0].comentarios").value(1))
            .andExpect(jsonPath("$[0].anexoNome").value("arquivo.bin"));
        mvc.perform(get("/api/posts/1/anexo").header("Authorization", AUTH))
            .andExpect(status().isOk()).andExpect(content().bytes(new byte[]{0,1,2,(byte)255}));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"ANA@EXAMPLE.COM\",\"senha\":\" senha123 \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").exists());
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Bia\",\"email\":\"bia@example.com\",\"senha\":\"senha123\"}"))
            .andExpect(status().isCreated());
    }

    private void inserirDados(Connection c, boolean v2) throws Exception {
        String hash = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(" senha123 ");
        String tabela = v2 ? "conta" : "talk_usuaria";
        String colunas = v2 ? "nome,email,senha_hash" : "nome,email,senha_hash,competencias,membro";
        for (var pessoa : List.of(new String[]{"Ana","Java","true"}, new String[]{"Lia","","false"},
                new String[]{"Eva","  SQL e análise  ","false"})) {
            String parametros = v2 ? "?,?,?" : "?,?,?,?,?";
            try (var s = c.prepareStatement("INSERT INTO " + tabela + "(" + colunas + ") VALUES (" + parametros + ") RETURNING id")) {
                s.setString(1, pessoa[0]); s.setString(2, pessoa[0].toLowerCase(java.util.Locale.ROOT) + "@example.com"); s.setString(3, hash);
                if (!v2) { s.setString(4, pessoa[1]); s.setBoolean(5, Boolean.parseBoolean(pessoa[2])); }
                try (var r = s.executeQuery()) {
                    r.next();
                    if (v2) {
                        try (var perfil = c.prepareStatement("INSERT INTO usuaria VALUES (?,?)")) {
                            perfil.setLong(1, r.getLong(1)); perfil.setString(2, pessoa[1]); perfil.executeUpdate();
                        }
                        if (consultar(c, "SELECT to_regclass('public.conta_papel')::text").getFirst().getFirst() != null) {
                            try (var papel = c.prepareStatement("INSERT INTO conta_papel VALUES (?,'USUARIA')")) {
                                papel.setLong(1, r.getLong(1)); papel.executeUpdate();
                            }
                        }
                    }
                }
            }
        }
        try (var s = c.createStatement()) {
            if (v2) s.executeUpdate("INSERT INTO talk_participacao VALUES (1)");
            String sessao = v2 ? "auth_sessao" : "talk_sessao";
            s.executeUpdate("INSERT INTO " + sessao + " VALUES ('" + HASH_TOKEN + "',1,CURRENT_TIMESTAMP + INTERVAL '8 hours')");
            String hashLia = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest("b".repeat(64).getBytes(StandardCharsets.US_ASCII)));
            s.executeUpdate("INSERT INTO " + sessao + " VALUES ('" + hashLia + "',2,CURRENT_TIMESTAMP + INTERVAL '8 hours')");
            s.executeUpdate("INSERT INTO " + sessao + " VALUES ('" + "f".repeat(64) + "',3,CURRENT_TIMESTAMP - INTERVAL '1 hour')");
            s.executeUpdate("INSERT INTO talk_post(autora_id,texto) VALUES (1,'Publicação anterior')");
            s.executeUpdate("INSERT INTO talk_comentario(post_id,autora_id,texto) VALUES (1,2,'Comentário anterior')");
            s.executeUpdate("INSERT INTO talk_curtida VALUES (1,1)");
            s.executeUpdate("INSERT INTO talk_anexo VALUES (1,'arquivo.bin','application/octet-stream',decode('000102ff','hex'))");
            // Lacuna intencional: a migração não pode reiniciar a sequência em max(id).
            s.execute("SELECT nextval('talk_usuaria_id_seq')");
        }
    }

    private ConfigurableApplicationContext iniciar(String banco) {
        return new SpringApplicationBuilder(CyberladiesApplication.class).run(
            "--spring.datasource.url=" + url(banco), "--spring.datasource.username=" + usuario(),
            "--spring.datasource.password=" + senha(), "--server.port=0", "--debug=false", "--logging.level.root=WARN");
    }

    private List<List<String>> estado(String banco) throws Exception {
        var estado = new ArrayList<>(dados(banco));
        try (var c = conectar(banco)) {
            estado.addAll(historico(c));
            if (consultar(c, "SELECT to_regclass('public.conta_papel')::text").getFirst().getFirst() != null)
                estado.addAll(consultar(c, "SELECT conta_id::text,papel FROM conta_papel ORDER BY conta_id,papel"));
        }
        return estado;
    }

    private List<List<String>> dados(String banco) throws Exception {
        var linhas = new ArrayList<List<String>>();
        try (var c = conectar(banco)) {
            boolean v2 = consultar(c, "SELECT to_regclass('public.conta')::text").getFirst().getFirst() != null;
            for (String tabela : TABELAS) {
                String origem = tabela;
                if (v2 && tabela.equals("talk_sessao")) origem = "auth_sessao";
                if (v2 && tabela.equals("talk_usuaria")) origem = """
                    (SELECT c.id,c.nome,c.email,c.senha_hash,u.competencias,
                     EXISTS(SELECT 1 FROM talk_participacao p WHERE p.conta_id=c.id) AS membro
                     FROM conta c JOIN usuaria u ON u.conta_id=c.id)
                    """;
                linhas.addAll(consultar(c, "SELECT '" + tabela + "',row_to_json(t)::text FROM " + origem + " t ORDER BY row_to_json(t)::text"));
            }
            for (String sequencia : List.of("talk_usuaria_id_seq", "talk_post_id_seq", "talk_comentario_id_seq")) {
                linhas.addAll(consultar(c, "SELECT '" + sequencia + "',last_value::text,is_called::text FROM " + sequencia));
            }
        }
        return linhas;
    }

    private List<List<String>> historico(Connection c) throws Exception {
        return consultar(c, "SELECT type,version,checksum::text,installed_on::text,success::text FROM flyway_schema_history ORDER BY installed_rank");
    }

    private List<List<String>> consultar(Connection c, String sql) throws Exception {
        var linhas = new ArrayList<List<String>>();
        try (var s = c.createStatement(); var r = s.executeQuery(sql)) {
            while (r.next()) {
                var linha = new ArrayList<String>();
                for (int i=1; i<=r.getMetaData().getColumnCount(); i++) linha.add(r.getString(i));
                linhas.add(linha);
            }
        }
        return linhas;
    }

    private void assertVazio(String banco) throws Exception {
        try (var c = conectar(banco)) {
            assertEquals("0", consultar(c, "SELECT count(*)::text FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public'").getFirst().getFirst(),
                "Use bancos de teste novos e vazios; estes testes não apagam bancos.");
        }
    }

    private void assertSemHistorico(String banco) throws Exception {
        try (var c = conectar(banco)) { assertNull(consultar(c, "SELECT to_regclass('public.flyway_schema_history')::text").getFirst().getFirst()); }
    }

    private void executar(String banco, String sql) throws Exception {
        try (var c = conectar(banco); var s = c.createStatement()) { s.execute(sql); }
    }

    private void adotar(String banco, String operacao) throws Exception { AdotarBanco.executar(url(banco), usuario(), senha(), operacao); }
    private Connection conectar(String banco) throws Exception { return DriverManager.getConnection(url(banco), usuario(), senha()); }
    private String url(String banco) { return "jdbc:postgresql://127.0.0.1:55439/" + banco; }
    private String usuario() { return System.getenv("DATABASE_USER"); }
    private String senha() { return System.getenv("DATABASE_PASSWORD"); }
}
