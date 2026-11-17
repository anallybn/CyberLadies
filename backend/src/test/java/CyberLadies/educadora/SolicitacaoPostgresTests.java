package CyberLadies.educadora;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class SolicitacaoPostgresTests {
    @Autowired WebApplicationContext contexto;
    @Autowired JdbcClient db;
    private MockMvc mvc;
    private long id;
    private String email;
    private String auth;
    private final List<Long> contas = new ArrayList<>();
    private static final String ENTRADA = "{\"especialidade\":\" Java \",\"instituicao\":\" Escola \"}";
    private static final String RESPOSTA = "{\"especialidade\":\"Java\",\"instituicao\":\"Escola\",\"status\":\"PENDENTE\"}";

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        email = UUID.randomUUID() + "@example.com";
        id = cadastrar(email);
        auth = login(email);
    }

    @AfterEach
    void limparFixtures() {
        db.sql("DROP TRIGGER IF EXISTS educadora_falha_teste ON conta_papel").update();
        db.sql("DROP FUNCTION IF EXISTS educadora_falha_teste()").update();
        for (long conta : contas) {
            db.sql("DELETE FROM talk_post WHERE autora_id=?").param(conta).update();
            db.sql("DELETE FROM conta WHERE id=?").param(conta).update();
        }
    }

    @Test
    void criaPendenteMantemContaSessaoParticipacaoELogin() throws Exception {
        mvc.perform(put("/api/comunidade/adesao").header("Authorization", auth)).andExpect(status().isNoContent());
        String contaAntes = db.sql("SELECT row_to_json(c)::text FROM conta c WHERE id=?").param(id).query(String.class).single();
        var sessoesAntes = db.sql("SELECT row_to_json(s)::text FROM auth_sessao s WHERE usuaria_id=? ORDER BY token_hash")
            .param(id).query(String.class).list();
        solicitar(auth).andExpect(status().isCreated()).andExpect(content().json(RESPOSTA, JsonCompareMode.STRICT));
        mvc.perform(get("/api/educadora/solicitacao").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(content().json(RESPOSTA, JsonCompareMode.STRICT));
        assertEquals(contaAntes, db.sql("SELECT row_to_json(c)::text FROM conta c WHERE id=?").param(id).query(String.class).single());
        assertEquals(sessoesAntes, db.sql("SELECT row_to_json(s)::text FROM auth_sessao s WHERE usuaria_id=? ORDER BY token_hash")
            .param(id).query(String.class).list());
        assertEquals(List.of("EDUCADORA","USUARIA"), papeis());
        assertEquals(1, db.sql("SELECT count(*) FROM talk_participacao WHERE conta_id=?").param(id).query(Integer.class).single());
        mvc.perform(get("/api/usuaria").header("Authorization", auth)).andExpect(status().isOk()).andExpect(jsonPath("$.membro").value(true));
        mvc.perform(get("/api/posts").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", login(email))).andExpect(status().isOk())
            .andExpect(jsonPath("$.papeis.length()").value(2));
    }

    @Test
    void rejeitaCamposInvalidosEControleDeContaStatusOuPapeis() throws Exception {
        for (String entrada : new String[]{"{}", "{\"especialidade\":\" \",\"instituicao\":\"Escola\"}",
                "{\"especialidade\":\"Java\",\"instituicao\":\" \"}",
                "{\"especialidade\":\"" + "x".repeat(121) + "\",\"instituicao\":\"Escola\"}",
                "{\"especialidade\":\"Java\",\"instituicao\":\"" + "x".repeat(201) + "\"}"}) {
            mvc.perform(post("/api/educadora/solicitacao").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(entrada)).andExpect(status().isBadRequest());
        }
        for (String extra : new String[]{"\"status\":\"APROVADA\"", "\"papeis\":[\"ADMIN\"]",
                "\"conta_id\":9999", "\"contaId\":9999", "\"email\":\"outra@example.com\"", "\"senha\":\"novaSenha\""}) {
            mvc.perform(post("/api/educadora/solicitacao").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(ENTRADA.substring(0,ENTRADA.length()-1) + "," + extra + "}"))
                .andExpect(status().isBadRequest());
        }
        semSolicitacaoOuPapel();
    }

    @Test
    void exigeSessaoEContaPessoalComUsuaria() throws Exception {
        for (String header : new String[]{"", "Bearer invalido", "Bearer " + "b".repeat(64)}) {
            solicitar(header).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/educadora/solicitacao").header("Authorization",header)).andExpect(status().isUnauthorized());
        }
        db.sql("DELETE FROM conta_papel WHERE conta_id=?").param(id).update();
        solicitar(auth).andExpect(status().isForbidden());
        db.sql("INSERT INTO conta_papel VALUES (?,'EMPRESA')").param(id).update();
        solicitar(auth).andExpect(status().isForbidden());
        mvc.perform(get("/api/educadora/solicitacao").header("Authorization",auth)).andExpect(status().isForbidden());
        assertEquals(0, db.sql("SELECT count(*) FROM educadora WHERE conta_id=?").param(id).query(Integer.class).single());
        assertEquals(List.of("EMPRESA"), papeis());
    }

    @Test
    void repeticaoRetornaConflitoSemAlterarDadosNemAprovacaoExistente() throws Exception {
        solicitar(auth).andExpect(status().isCreated());
        for (String status : new String[]{"PENDENTE","APROVADA","REJEITADA"}) {
            db.sql("UPDATE educadora SET status=? WHERE conta_id=?").params(status,id).update();
            mvc.perform(post("/api/educadora/solicitacao").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"especialidade\":\"Outra\",\"instituicao\":\"Outra escola\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Solicitação de educadora já cadastrada."));
            mvc.perform(get("/api/educadora/solicitacao").header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.especialidade").value("Java"))
                .andExpect(jsonPath("$.instituicao").value("Escola")).andExpect(jsonPath("$.status").value(status));
        }
        assertEquals(1, db.sql("SELECT count(*) FROM educadora WHERE conta_id=?").param(id).query(Integer.class).single());
        assertEquals(List.of("EDUCADORA","USUARIA"), papeis());
    }

    @Test
    void solicitacoesConcorrentesCriamUmaSoSolicitacao() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var inicio = new CountDownLatch(1);
            var primeira = executor.submit(() -> { inicio.await(); return solicitar(auth).andReturn().getResponse().getStatus(); });
            var segunda = executor.submit(() -> { inicio.await(); return solicitar(auth).andReturn().getResponse().getStatus(); });
            inicio.countDown();
            var resultados = new ArrayList<>(List.of(primeira.get(10,TimeUnit.SECONDS),segunda.get(10,TimeUnit.SECONDS)));
            resultados.sort(Integer::compareTo);
            assertEquals(List.of(201,409), resultados);
        }
        assertEquals(1, db.sql("SELECT count(*) FROM educadora WHERE conta_id=?").param(id).query(Integer.class).single());
        assertEquals(List.of("EDUCADORA","USUARIA"), papeis());
    }

    @Test
    void falhaNaAtribuicaoRevertePerfilEPapel() throws Exception {
        db.sql("""
            CREATE FUNCTION educadora_falha_teste() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
                IF NEW.conta_id=%d AND NEW.papel='EDUCADORA' THEN
                    RAISE EXCEPTION 'Falha simulada na atribuição';
                END IF;
                RETURN NEW;
            END; $$
            """.formatted(id)).update();
        db.sql("CREATE TRIGGER educadora_falha_teste BEFORE INSERT ON conta_papel FOR EACH ROW EXECUTE FUNCTION educadora_falha_teste()")
            .update();
        assertThrows(jakarta.servlet.ServletException.class, () -> solicitar(auth));
        semSolicitacaoOuPapel();
    }

    @Test
    void consultaNaoPermiteVerOuCriarSolicitacaoDeOutraConta() throws Exception {
        solicitar(auth).andExpect(status().isCreated());
        String outraEmail = UUID.randomUUID() + "@example.com";
        cadastrar(outraEmail);
        String outraAuth = login(outraEmail);
        mvc.perform(get("/api/educadora/solicitacao").header("Authorization",outraAuth))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Solicitação de educadora não encontrada."));
        mvc.perform(get("/api/educadora/solicitacao").queryParam("conta_id",Long.toString(id)).header("Authorization",outraAuth))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/educadora/solicitacao/" + id).header("Authorization",outraAuth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/educadora/solicitacao").header("Authorization",outraAuth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"especialidade\":\"Outra\",\"instituicao\":\"Outra\",\"conta_id\":" + id + "}"))
            .andExpect(status().isBadRequest());
        assertEquals(1, db.sql("SELECT count(*) FROM educadora").query(Integer.class).single());
    }

    private org.springframework.test.web.servlet.ResultActions solicitar(String autorizacao) throws Exception {
        return mvc.perform(post("/api/educadora/solicitacao").header("Authorization",autorizacao)
            .contentType(MediaType.APPLICATION_JSON).content(ENTRADA));
    }

    private List<String> papeis() {
        return db.sql("SELECT papel FROM conta_papel WHERE conta_id=? ORDER BY papel").param(id).query(String.class).list();
    }

    private void semSolicitacaoOuPapel() {
        assertEquals(0, db.sql("SELECT count(*) FROM educadora WHERE conta_id=?").param(id).query(Integer.class).single());
        assertEquals(List.of("USUARIA"), papeis());
    }

    private long cadastrar(String endereco) throws Exception {
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Ana\",\"email\":\"" + endereco + "\",\"senha\":\" senha123 \"}"))
            .andExpect(status().isCreated());
        long conta = db.sql("SELECT id FROM conta WHERE email=?").param(endereco).query(Long.class).single();
        contas.add(conta);
        return conta;
    }

    private String login(String endereco) throws Exception {
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + endereco + "\",\"senha\":\" senha123 \"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + resposta.split("\"")[3];
    }
}
