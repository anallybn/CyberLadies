package CyberLadies.talk;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contratos da origem reconciliados com papéis e solicitação atuais. */
@SpringBootTest @AutoConfigureMockMvc @Transactional
@EnabledIfEnvironmentVariable(named="DATABASE_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class AuthPerfisIntegrationTest {
    @Autowired MockMvc mvc; @Autowired JdbcClient db;
    String email() { return UUID.randomUUID()+"@example.invalid"; }
    String senha="SenhaFixture123";
    String dados(String email) { return "{\"nome\":\"Fixture\",\"email\":\""+email+"\",\"senha\":\""+senha+"\"}"; }
    String login(String email) throws Exception {
        String json=mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\""+email+"\",\"senha\":\""+senha+"\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer "+JsonMapper.builder().build().readTree(json).get("token").asString();
    }
    @Test void contaPessoalMantemPapéisEProjecaoSemCredenciais() throws Exception {
        String email=email();
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType("application/json").content(dados(email))).andExpect(status().isCreated());
        String auth=login(email);
        mvc.perform(get("/api/auth/me").header("Authorization",auth)).andExpect(jsonPath("$.papeis[0]").value("USUARIA"));
        mvc.perform(get("/api/auth/conta").header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.perfil").value("CYBERLADIE")).andExpect(jsonPath("$.senhaHash").doesNotExist());
    }
    @Test void educadoraUsaSolicitacaoSemCadastroParaleloEAcumulaPapeis() throws Exception {
        String email=email();
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType("application/json").content(dados(email))).andExpect(status().isCreated());
        String auth=login(email);
        mvc.perform(post("/api/auth/cadastro/educadora").contentType("application/json").content(dados(email))).andExpect(status().isNotFound());
        mvc.perform(post("/api/educadora/solicitacao").header("Authorization",auth).contentType("application/json")
            .content("{\"especialidade\":\"Java\",\"instituicao\":\"Teste\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/auth/conta").header("Authorization",auth)).andExpect(jsonPath("$.perfil").value("EDUCADORA"))
            .andExpect(jsonPath("$.statusEducadora").value("PENDENTE"));
        mvc.perform(get("/api/candidaturas").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/educadora/alunas").header("Authorization",auth)).andExpect(status().isForbidden());
    }
    @Test void cadastroEmpresaUsaContaAtualSemGanharAcessoDeEstudante() throws Exception {
        String email=email();
        String dados=dados(email).replace("}",",\"cnpj\":\"11222333000181\",\"areaAtuacao\":\"Tecnologia\"}");
        mvc.perform(post("/api/auth/cadastro/empresa").contentType("application/json").content(dados)).andExpect(status().isCreated());
        String auth=login(email);
        mvc.perform(get("/api/auth/me").header("Authorization",auth)).andExpect(jsonPath("$.papeis[0]").value("EMPRESA"));
        mvc.perform(get("/api/auth/conta").header("Authorization",auth)).andExpect(jsonPath("$.perfil").value("EMPRESA"));
        mvc.perform(get("/api/candidaturas").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/perfil").header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.dadosProfissionais").value(false));
        mvc.perform(get("/api/academy/me/progresso").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/cadastro/empresa").contentType("application/json")
            .content(dados.replace(email,email()))).andExpect(status().isConflict());
    }
    @Test void rejeitaElevacaoDePapelECnpjInvalidoSemSalvarConta() throws Exception {
        String email=email();
        mvc.perform(post("/api/auth/cadastro/empresa").contentType("application/json")
            .content(dados(email).replace("}",",\"cnpj\":\"00000000000000\",\"areaAtuacao\":\"Teste\"}"))).andExpect(status().isBadRequest());
        assertThat(db.sql("SELECT count(*) FROM conta WHERE email=?").param(email).query(Long.class).single()).isZero();
        mvc.perform(post("/api/auth/cadastro/empresa").contentType("application/json")
            .content(dados(email).replace("}",",\"cnpj\":\"11222333000181\",\"areaAtuacao\":\"Teste\",\"papeis\":[\"ADMIN\"]}"))).andExpect(status().isBadRequest());
    }
    @Test void logoutInvalidaAmbasAsProjecoes() throws Exception {
        String email=email();
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType("application/json").content(dados(email))).andExpect(status().isCreated());
        String auth=login(email);
        mvc.perform(delete("/api/auth/sessao").header("Authorization",auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization",auth)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/conta").header("Authorization",auth)).andExpect(status().isUnauthorized());
    }
}
