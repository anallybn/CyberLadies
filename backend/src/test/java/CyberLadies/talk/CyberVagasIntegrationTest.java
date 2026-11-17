package CyberLadies.talk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.test.context.jdbc.Sql(scripts="/db/fixture-conta-integracao.sql", executionPhase=org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS, config=@org.springframework.test.context.jdbc.SqlConfig(separator="@@@"))
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Fixtures exclusivamente de teste: revertidas ao final de cada método.
class CyberVagasIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    long usuaria, outra, vaga, empresa;
    String auth = "Bearer " + "a".repeat(64);
    String authOutra = "Bearer " + "b".repeat(64);
    String marcador;

    @BeforeEach
    void preparar() {
        marcador = "fixture-" + UUID.randomUUID();
        usuaria = criarConta("candidata");
        outra = criarConta("outra");
        sessao(auth, usuaria);
        sessao(authOutra, outra);
        empresa = db.sql("INSERT INTO cyber_empresa(nome) VALUES (?) RETURNING id")
            .param(marcador).query(Long.class).single();
        vaga = criarVaga("JUNIOR", "REMOTO", true, true, true, 8000);
        criarVaga("PLENO", "HIBRIDO", false, false, false, 5000);
        criarVaga("SENIOR", "PRESENCIAL", false, true, false, 10000);
        criarVaga("ESTAGIO", "REMOTO", true, false, true, 2000);
    }

    long criarConta(String nome) {
        return db.sql("SELECT fixture_conta_integracao(?,?,'fixture','','CYBERLADIE',true)")
            .params(nome, UUID.randomUUID()+"@example.invalid").query(Long.class).single();
    }
    void sessao(String token, long id) {
        db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP + INTERVAL '1 hour')")
            .params(CyberLadies.auth.service.Credenciais.hashBearer(token),id).update();
    }
    long criarVaga(String nivel, String modalidade, boolean maes, boolean pcd, boolean formacao, int salario) {
        return db.sql("""
            INSERT INTO cyber_vaga(empresa_id,titulo,descricao,requisitos,nivel,modalidade,
                salario_minimo,salario_maximo,tecnologias,para_maes,pcd,sem_formacao)
            VALUES (?,?,'Descrição','Requisitos',?,?,1000,?,'React,Java',?,?,?) RETURNING id
            """).params(empresa,marcador+" "+nivel,nivel,modalidade,salario,maes,pcd,formacao)
            .query(Long.class).single();
    }
    @Test void consultaPublicaECombinacaoDeTodosOsFiltros() throws Exception {
        mvc.perform(get("/api/vagas").param("busca",marcador))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalResultados").value(4));
        mvc.perform(get("/api/vagas").param("busca",marcador)
            .param("nivel","JUNIOR","PLENO").param("modalidade","REMOTO","HIBRIDO")
            .param("salarioMinimo","7000").param("paraMaes","true").param("pcd","true").param("semFormacao","true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalResultados").value(1))
            .andExpect(jsonPath("$.vagas[0].id").value(vaga));
        mvc.perform(get("/api/vagas/"+vaga)).andExpect(status().isOk())
            .andExpect(jsonPath("$.empresa").value(marcador));
        for (String nivel : new String[]{"ESTAGIO","JUNIOR","PLENO","SENIOR"})
            mvc.perform(get("/api/vagas").param("busca",marcador).param("nivel",nivel))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalResultados").value(1));
        for (String modalidade : new String[]{"REMOTO","HIBRIDO","PRESENCIAL"})
            mvc.perform(get("/api/vagas").param("busca",marcador).param("modalidade",modalidade))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalResultados").value(modalidade.equals("REMOTO") ? 2 : 1));
    }
    @Test void validacaoEstadosVaziosEEscritaBloqueada() throws Exception {
        mvc.perform(get("/api/vagas").param("nivel","INVALIDO")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vagas").param("salarioMinimo","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vagas").param("salarioMinimo","abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vagas").param("busca","x".repeat(151))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vagas").param("busca",marcador).param("salarioMinimo","20000"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.vagas").isEmpty());
        mvc.perform(get("/api/vagas/9223372036854775807")).andExpect(status().isNotFound());
        mvc.perform(post("/api/vagas").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/vagas/"+vaga).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/vagas/"+vaga)).andExpect(status().isForbidden());
    }
    @Test void candidaturaPersistidaDuplicadaEIsolamentoDeContas() throws Exception {
        String corpo="{\"vagaId\":"+vaga+",\"usuariaId\":"+outra+",\"status\":\"APROVADA\"}";
        mvc.perform(post("/api/candidaturas").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.usuariaId").value(usuaria))
            .andExpect(jsonPath("$.status").value("PENDENTE"));
        long id=db.sql("SELECT id FROM cyber_candidatura WHERE usuaria_id=? AND vaga_id=?")
            .params(usuaria,vaga).query(Long.class).single();
        assertThat(db.sql("SELECT count(*) FROM cyber_candidatura WHERE usuaria_id=? AND vaga_id=?")
            .params(usuaria,vaga).query(Long.class).single()).isEqualTo(1);
        mvc.perform(post("/api/candidaturas").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/candidaturas").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].vagaId").value(vaga));
        mvc.perform(get("/api/candidaturas").header("Authorization",authOutra))
            .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/candidaturas/"+id).header("Authorization",authOutra)).andExpect(status().isNotFound());
        mvc.perform(get("/api/candidaturas/"+id).header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(put("/api/candidaturas/"+id).header("Authorization",authOutra)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/candidaturas/"+id).header("Authorization",authOutra)).andExpect(status().isForbidden());
    }
    @Test void sessaoObrigatoriaEImpossibilidadeDeCandidatarAVagaEncerrada() throws Exception {
        String corpo="{\"vagaId\":"+vaga+"}";
        mvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/candidaturas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/candidaturas").header("Authorization","Bearer invalido")
            .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isUnauthorized());
        db.sql("UPDATE auth_sessao SET expira_em=CURRENT_TIMESTAMP - INTERVAL '1 hour' WHERE usuaria_id=?").param(outra).update();
        mvc.perform(get("/api/candidaturas").header("Authorization",authOutra)).andExpect(status().isUnauthorized());
        db.sql("UPDATE cyber_vaga SET ativa=false WHERE id=?").param(vaga).update();
        mvc.perform(post("/api/candidaturas").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isNotFound());
        mvc.perform(post("/api/candidaturas").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":0}"))
            .andExpect(status().isBadRequest());
    }
    @Test void cyberTalkContinuaOperandoComAMesmaSessao() throws Exception {
        mvc.perform(get("/api/usuaria").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(post("/api/posts").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"texto\":\""+marcador+"\"}")).andExpect(status().isCreated());
        long post=db.sql("SELECT id FROM talk_post WHERE autora_id=? AND texto=?").params(usuaria,marcador).query(Long.class).single();
        mvc.perform(put("/api/posts/"+post).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"texto\":\"Editado\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/posts/"+post+"/curtida").header("Authorization",authOutra)).andExpect(status().isOk());
        mvc.perform(post("/api/posts/"+post+"/comentarios").header("Authorization",authOutra)
            .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Comentário\"}")).andExpect(status().isCreated());
        mvc.perform(delete("/api/posts/"+post).header("Authorization",authOutra)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/posts/"+post).header("Authorization",auth)).andExpect(status().isNoContent());
    }
}