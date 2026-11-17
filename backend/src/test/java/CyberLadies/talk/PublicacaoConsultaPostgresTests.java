package CyberLadies.talk;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class PublicacaoConsultaPostgresTests {
    @Autowired WebApplicationContext contexto;
    @Autowired JdbcClient db;
    private MockMvc mvc;
    private long ana;
    private long bia;
    private long visitante;
    private static final String ANA = "Bearer " + "1".repeat(64);
    private static final String BIA = "Bearer " + "2".repeat(64);
    private static final String VISITANTE = "Bearer " + "3".repeat(64);

    @BeforeEach
    void configurar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        ana = usuaria("Ana", true, ANA);
        bia = usuaria("Bia", true, BIA);
        visitante = usuaria("Visitante", false, VISITANTE);
    }

    private long usuaria(String nome, boolean membro, String auth) throws Exception {
        long id = db.sql("INSERT INTO conta(nome,email,senha_hash) VALUES (?,?,?) RETURNING id")
            .params(nome, nome + "@example.com", "nao-utilizado").query(Long.class).single();
        db.sql("INSERT INTO usuaria(conta_id,competencias) VALUES (?,'')").param(id).update();
        if (membro) db.sql("INSERT INTO talk_participacao(conta_id) VALUES (?)").param(id).update();
        db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP + INTERVAL '8 hours')")
            .params(hashToken(auth), id).update();
        return id;
    }

    private long publicar(long autora, String texto, String data) {
        return db.sql("INSERT INTO talk_post(autora_id,texto,criado_em) VALUES (?,?,?) RETURNING id")
            .params(autora, texto, OffsetDateTime.parse(data)).query(Long.class).single();
    }

    // Gera o hash no formato persistido anterior, sem expor detalhes internos de auth ao Talk.
    private String hashToken(String auth) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(auth.substring(7).getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void listaVazia() throws Exception {
        mvc.perform(get("/api/posts").header("Authorization", ANA))
            .andExpect(status().isOk()).andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void ordenaPorDataDescendenteEDesempataPorIdDescendente() throws Exception {
        long antigo = publicar(ana, "Antigo", "2026-01-01T12:00:00Z");
        long primeiro = publicar(bia, "Primeiro empate", "2026-01-02T12:00:00Z");
        long segundo = publicar(ana, "Segundo empate", "2026-01-02T12:00:00Z");
        mvc.perform(get("/api/posts").header("Authorization", ANA))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(segundo)).andExpect(jsonPath("$[1].id").value(primeiro))
            .andExpect(jsonPath("$[2].id").value(antigo));
    }

    @Test
    void consultaEListaPreservamRespostaCompletaEIndicadoresPorUsuaria() throws Exception {
        long id = publicar(ana, "Texto com anexo", "2026-01-02T12:00:00Z");
        db.sql("INSERT INTO talk_anexo(post_id,nome,tipo,dados) VALUES (?,'foto.png','image/png',?)")
            .params(id, new byte[]{1, 2, 3}).update();
        db.sql("INSERT INTO talk_curtida VALUES (?,?),(?,?)").params(id, ana, id, visitante).update();
        db.sql("INSERT INTO talk_comentario(post_id,autora_id,texto) VALUES (?,?,'Um'),(?,?,'Dois')")
            .params(id, ana, id, bia).update();
        String resposta = """
            {"id":%d,"autoraId":%d,"autora":"Ana","texto":"Texto com anexo","criadoEm":"2026-01-02T12:00:00Z",
             "propria":true,"curtido":true,"curtidas":2,"comentarios":2,"anexoNome":"foto.png","anexoTipo":"image/png"}
            """.formatted(id, ana);
        mvc.perform(get("/api/posts/{id}", id).header("Authorization", ANA))
            .andExpect(status().isOk()).andExpect(content().json(resposta, JsonCompareMode.STRICT));
        mvc.perform(get("/api/posts").header("Authorization", ANA))
            .andExpect(status().isOk()).andExpect(content().json("[" + resposta + "]", JsonCompareMode.STRICT));
        String respostaBia = resposta.replace("\"propria\":true", "\"propria\":false").replace("\"curtido\":true", "\"curtido\":false");
        mvc.perform(get("/api/posts/{id}", id).header("Authorization", BIA))
            .andExpect(status().isOk()).andExpect(content().json(respostaBia, JsonCompareMode.STRICT));
        mvc.perform(get("/api/posts").header("Authorization", BIA))
            .andExpect(status().isOk()).andExpect(content().json("[" + respostaBia + "]", JsonCompareMode.STRICT));
    }

    @Test
    void semAnexoECurtidasOuComentariosMantemNulosEZeros() throws Exception {
        long id = publicar(bia, "Sem anexo", "2026-01-01T12:00:00Z");
        String resposta = """
            {"id":%d,"autoraId":%d,"autora":"Bia","texto":"Sem anexo","criadoEm":"2026-01-01T12:00:00Z",
             "propria":false,"curtido":false,"curtidas":0,"comentarios":0,"anexoNome":null,"anexoTipo":null}
            """.formatted(id, bia);
        mvc.perform(get("/api/posts/{id}", id).header("Authorization", ANA))
            .andExpect(status().isOk()).andExpect(content().json(resposta, JsonCompareMode.STRICT));
        mvc.perform(get("/api/posts").header("Authorization", ANA))
            .andExpect(status().isOk()).andExpect(content().json("[" + resposta + "]", JsonCompareMode.STRICT));
    }

    @Test
    void publicacaoInexistente() throws Exception {
        mvc.perform(get("/api/posts/{id}", Long.MAX_VALUE).header("Authorization", ANA))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Publicação não encontrada."));
    }

    @Test
    void exigeSessaoValidaEParticipacaoAntesDeConsultar() throws Exception {
        long id = publicar(ana, "Restrita", "2026-01-01T12:00:00Z");
        String expirada = "Bearer " + "4".repeat(64);
        db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP)")
            .params(hashToken(expirada), ana).update();
        for (String rota : new String[]{"/api/posts", "/api/posts/" + id, "/api/posts/" + Long.MAX_VALUE}) {
            mvc.perform(get(rota)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Entre para acessar o CyberTalk."));
            mvc.perform(get(rota).header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Entre para acessar o CyberTalk."));
            for (String auth : new String[]{expirada, "Bearer " + "5".repeat(64)}) {
                mvc.perform(get(rota).header("Authorization", auth)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Sessão expirada."));
            }
            mvc.perform(get(rota).header("Authorization", VISITANTE)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Entre na comunidade primeiro."));
        }
    }

    @Test
    void identidadeCompartilhadaPreservaRestricaoDeAutoria() throws Exception {
        long id = publicar(ana, "Da Ana", "2026-01-01T12:00:00Z");
        mvc.perform(put("/api/posts/{id}", id).header("Authorization", BIA)
            .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Alterada\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Apenas a autora pode alterar a publicação."));
        mvc.perform(delete("/api/posts/{id}", id).header("Authorization", BIA))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Apenas a autora pode alterar a publicação."));
        mvc.perform(get("/api/posts/{id}", id).header("Authorization", ANA))
            .andExpect(status().isOk()).andExpect(jsonPath("$.texto").value("Da Ana"));
    }

    @Test
    void leituraCompartilhadaPreservaRespostasDasOperacoesExistentes() throws Exception {
        mvc.perform(post("/api/posts").header("Authorization", ANA).contentType(MediaType.APPLICATION_JSON)
            .content("{\"texto\":\"  Nova  \"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.texto").value("Nova"))
            .andExpect(jsonPath("$.propria").value(true));
        long id = db.sql("SELECT id FROM talk_post WHERE texto='Nova'").query(Long.class).single();
        mvc.perform(put("/api/posts/{id}", id).header("Authorization", ANA).contentType(MediaType.APPLICATION_JSON)
            .content("{\"texto\":\" Editada \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.texto").value("Editada"));
        mvc.perform(put("/api/posts/{id}/curtida", id).header("Authorization", BIA))
            .andExpect(status().isOk()).andExpect(jsonPath("$.curtido").value(true))
            .andExpect(jsonPath("$.curtidas").value(1)).andExpect(jsonPath("$.propria").value(false));
        mvc.perform(delete("/api/posts/{id}/curtida", id).header("Authorization", BIA))
            .andExpect(status().isOk()).andExpect(jsonPath("$.curtido").value(false)).andExpect(jsonPath("$.curtidas").value(0));
        mvc.perform(post("/api/posts/{id}/comentarios", id).header("Authorization", BIA).contentType(MediaType.APPLICATION_JSON)
            .content("{\"texto\":\"Oi\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/posts/{id}/comentarios", id).header("Authorization", ANA))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].texto").value("Oi"));
        mvc.perform(multipart("/api/posts").file(new MockMultipartFile("arquivo", "arquivo.txt", "text/plain", new byte[]{1}))
            .param("texto", "Anexo").header("Authorization", ANA))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.anexoNome").value("arquivo.txt"))
            .andExpect(jsonPath("$.anexoTipo").value("application/octet-stream"));
    }
}
