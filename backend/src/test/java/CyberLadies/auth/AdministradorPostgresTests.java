package CyberLadies.auth;

import java.nio.CharBuffer;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class AdministradorPostgresTests {
    @Autowired WebApplicationContext contexto;
    @Autowired JdbcClient db;
    @Autowired PasswordEncoder encoder;
    private MockMvc mvc;
    private String email;
    private char[] senha;
    private final List<Long> contas = new ArrayList<>();

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        email = UUID.randomUUID() + "@example.com";
        // Credencial aleatória exclusiva de teste, nunca usada pelo comando em produção.
        senha = (" " + UUID.randomUUID() + " ").toCharArray();
    }

    @AfterEach
    void limparFixtures() {
        Arrays.fill(senha,'\0');
        db.sql("DROP TRIGGER IF EXISTS admin_falha_teste ON conta_papel").update();
        db.sql("DROP FUNCTION IF EXISTS admin_falha_teste()").update();
        for (long id : contas) db.sql("DELETE FROM conta WHERE id=?").param(id).update();
    }

    @Test
    void criaSomenteAdminComHashCompativelLoginEMe() throws Exception {
        long id = criar("  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", "cyberladies_auth_test");
        var papeis = db.sql("SELECT papel FROM conta_papel WHERE conta_id=?").param(id).query(String.class).list();
        assertEquals(List.of("ADMIN"),papeis);
        for (String tabela : List.of("usuaria","educadora","talk_participacao")) {
            assertEquals(0,db.sql("SELECT count(*) FROM " + tabela + " WHERE conta_id=?").param(id).query(Integer.class).single());
        }
        assertEquals(0,db.sql("SELECT count(*) FROM auth_sessao WHERE usuaria_id=?").param(id).query(Integer.class).single());
        String hash = db.sql("SELECT senha_hash FROM conta WHERE id=?").param(id).query(String.class).single();
        assertNotEquals(new String(senha),hash);
        assertTrue(encoder.matches(CharBuffer.wrap(senha),hash));
        assertTrue(Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches(CharBuffer.wrap(senha),hash));
        String auth = login(email,new String(senha));
        mvc.perform(get("/api/auth/me").header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(content().json("{\"id\":" + id + ",\"papeis\":[\"ADMIN\"]}",JsonCompareMode.STRICT));
    }

    @Test
    void adminNaoGanhaAcoesDeOutrosPerfis() throws Exception {
        criar(email,"cyberladies_auth_test");
        String auth = login(email,new String(senha));
        for (String rota : List.of("/api/usuaria","/api/posts"))
            mvc.perform(get(rota).header("Authorization",auth)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/comunidade/adesao").header("Authorization",auth)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posts").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"texto\":\"Sem autorização\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/educadora/solicitacao").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"especialidade\":\"Java\",\"instituicao\":\"Escola\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/educadora/solicitacao").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").header("Authorization",auth)).andExpect(status().isOk());
    }

    @Test
    void emailExistenteNaoEPromovidoNemModificado() throws Exception {
        cadastrarPublicamente();
        long id = db.sql("SELECT id FROM conta WHERE email=?").param(email).query(Long.class).single();
        contas.add(id);
        String auth = login(email,new String(senha));
        mvc.perform(put("/api/comunidade/adesao").header("Authorization",auth)).andExpect(status().isNoContent());
        String antes = db.sql("SELECT row_to_json(c)::text FROM conta c WHERE id=?").param(id).query(String.class).single();
        var sessoes = db.sql("SELECT row_to_json(s)::text FROM auth_sessao s WHERE usuaria_id=?").param(id).query(String.class).list();
        assertThrows(IllegalArgumentException.class, () -> criar(" " + email.toUpperCase(java.util.Locale.ROOT) + " ","cyberladies_auth_test"));
        assertEquals(antes,db.sql("SELECT row_to_json(c)::text FROM conta c WHERE id=?").param(id).query(String.class).single());
        assertEquals(List.of("USUARIA"),db.sql("SELECT papel FROM conta_papel WHERE conta_id=?").param(id).query(String.class).list());
        assertEquals(sessoes,db.sql("SELECT row_to_json(s)::text FROM auth_sessao s WHERE usuaria_id=?").param(id).query(String.class).list());
        assertEquals(1,db.sql("SELECT count(*) FROM talk_participacao WHERE conta_id=?").param(id).query(Integer.class).single());
        assertEquals(1,db.sql("SELECT count(*) FROM usuaria WHERE conta_id=?").param(id).query(Integer.class).single());
    }

    @Test
    void falhaNoPapelReverteCriacaoDaConta() throws Exception {
        db.sql("""
            CREATE FUNCTION admin_falha_teste() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
                IF NEW.papel='ADMIN' THEN RAISE EXCEPTION 'Falha simulada de atribuição'; END IF;
                RETURN NEW;
            END; $$
            """).update();
        db.sql("CREATE TRIGGER admin_falha_teste BEFORE INSERT ON conta_papel FOR EACH ROW EXECUTE FUNCTION admin_falha_teste()")
            .update();
        assertThrows(DataAccessException.class, () -> criar(email,"cyberladies_auth_test"));
        assertEquals(0,db.sql("SELECT count(*) FROM conta WHERE email=?").param(email).query(Integer.class).single());
        assertEquals(0,db.sql("SELECT count(*) FROM conta_papel WHERE papel='ADMIN'").query(Integer.class).single());
    }

    @Test
    void cadastroPublicoNaoConcedeAdminENaoHaEndpointDeAdmin() throws Exception {
        cadastrarPublicamente();
        long id = db.sql("SELECT id FROM conta WHERE email=?").param(email).query(Long.class).single();
        contas.add(id);
        String auth = login(email,new String(senha));
        mvc.perform(get("/api/auth/me").header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(content().json("{\"id\":" + id + ",\"papeis\":[\"USUARIA\"]}",JsonCompareMode.STRICT));
        mvc.perform(post("/api/auth/cadastro/admin").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void destinoDiferenteERecusadoAntesDeCriarConta() {
        assertThrows(IllegalArgumentException.class, () -> criar(email,"outro_banco"));
        assertThrows(IllegalArgumentException.class, () -> criar(email,""));
        assertEquals(0,db.sql("SELECT count(*) FROM conta WHERE email=?").param(email).query(Integer.class).single());
    }

    @Test
    void comandoRecusaEntradaRedirecionadaSemConectarOuPedirSenha() throws Exception {
        String executavelJava = Path.of(System.getProperty("java.home"),"bin","java").toString();
        var processo = new ProcessBuilder(executavelJava,"-cp",System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),
            "CyberLadies.auth.CriarAdministrador","--banco=cyberladies_auth_test").redirectErrorStream(true).start();
        processo.getOutputStream().close();
        assertTrue(processo.waitFor(10,TimeUnit.SECONDS));
        String saida = new String(processo.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(1,processo.exitValue());
        assertTrue(saida.contains("terminal interativo"));
        assertFalse(saida.contains("Nome da conta"));
    }

    @Test
    void alteraSenhaSemDuplicarContaOuModificarIdentidadePapeisEDados() throws Exception {
        long id=criar(email,"cyberladies_auth_test");
        String antes=db.sql("SELECT (to_jsonb(c)-'senha_hash')::text FROM conta c WHERE id=?").param(id).query(String.class).single();
        var papeis=db.sql("SELECT papel FROM conta_papel WHERE conta_id=? ORDER BY papel").param(id).query(String.class).list();
        long quantidade=db.sql("SELECT count(*) FROM conta").query(Long.class).single();
        char[] nova=UUID.randomUUID().toString().toCharArray();
        try {
            try(var c=DriverManager.getConnection(System.getenv("DATABASE_URL"),System.getenv("DATABASE_USER"),System.getenv("DATABASE_PASSWORD"))) {
                assertEquals(id,AlterarSenhaAdministrador.alterar(c,encoder,"cyberladies_auth_test",email,nova));
            }
            assertEquals(antes,db.sql("SELECT (to_jsonb(c)-'senha_hash')::text FROM conta c WHERE id=?").param(id).query(String.class).single());
            assertEquals(papeis,db.sql("SELECT papel FROM conta_papel WHERE conta_id=? ORDER BY papel").param(id).query(String.class).list());
            assertEquals(quantidade,db.sql("SELECT count(*) FROM conta").query(Long.class).single());
            String auth=login(email,new String(nova));
            mvc.perform(get("/api/auth/me").header("Authorization",auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.papeis[0]").value("ADMIN"));
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+email+"\",\"senha\":\""+new String(senha)+"\"}")).andExpect(status().isUnauthorized());
        } finally { Arrays.fill(nova,'\0'); }
    }

    @Test
    void trocaDeSenhaRecusaDestinoIncorretoEContaInexistenteSemCriarNada() throws Exception {
        long id=criar(email,"cyberladies_auth_test");
        long quantidade=db.sql("SELECT count(*) FROM conta").query(Long.class).single();
        try(var c=DriverManager.getConnection(System.getenv("DATABASE_URL"),System.getenv("DATABASE_USER"),System.getenv("DATABASE_PASSWORD"))) {
            assertThrows(IllegalArgumentException.class,()->AlterarSenhaAdministrador.alterar(c,encoder,"outro_banco",email,senha));
        }
        try(var c=DriverManager.getConnection(System.getenv("DATABASE_URL"),System.getenv("DATABASE_USER"),System.getenv("DATABASE_PASSWORD"))) {
            assertThrows(IllegalArgumentException.class,()->AlterarSenhaAdministrador.alterar(c,encoder,"cyberladies_auth_test",UUID.randomUUID()+"@example.invalid",senha));
        }
        assertEquals(quantidade,db.sql("SELECT count(*) FROM conta").query(Long.class).single());
        assertTrue(encoder.matches(CharBuffer.wrap(senha),db.sql("SELECT senha_hash FROM conta WHERE id=?").param(id).query(String.class).single()));
    }

    @Test
    void trocaDeSenhaRecusaPipeSemConectarOuSolicitarSegredo() throws Exception {
        String executavel=Path.of(System.getProperty("java.home"),"bin","java").toString();
        var processo=new ProcessBuilder(executavel,"-cp",System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),
            "CyberLadies.auth.AlterarSenhaAdministrador","--email="+email).redirectErrorStream(true).start();
        processo.getOutputStream().close();
        assertTrue(processo.waitFor(10,TimeUnit.SECONDS));
        String saida=new String(processo.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(1,processo.exitValue());
        assertTrue(saida.contains("terminal interativo"));
        assertFalse(saida.contains("Nova senha"));
    }

    private long criar(String endereco, String banco) throws Exception {
        try (var c = DriverManager.getConnection(System.getenv("DATABASE_URL"),System.getenv("DATABASE_USER"),System.getenv("DATABASE_PASSWORD"))) {
            long id = AdministradorOperacao.criar(c,encoder,banco,"Operação de teste",endereco,senha);
            contas.add(id);
            return id;
        }
    }

    private void cadastrarPublicamente() throws Exception {
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Ana","email":"%s","senha":"%s","papeis":["ADMIN"],"perfil":"ADMIN","admin":true}
            """.formatted(email,new String(senha)))).andExpect(status().isCreated());
    }

    private String login(String endereco, String segredo) throws Exception {
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + endereco + "\",\"senha\":\"" + segredo + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + resposta.split("\"")[3];
    }
}
