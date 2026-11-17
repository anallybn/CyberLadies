package CyberLadies.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class SessaoPostgresTests {
    @Autowired WebApplicationContext contexto;
    @Autowired JdbcClient db;
    private MockMvc mvc;

    @BeforeEach
    void configurar() { mvc = MockMvcBuilders.webAppContextSetup(contexto).build(); }

    private void cadastrar() throws Exception {
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nome":"Ana","email":"ANA@EXAMPLE.COM","senha":" senha123 "}
                """))
            .andExpect(status().isCreated());
    }

    private String entrar() throws Exception {
        var resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"ANA@EXAMPLE.COM","senha":" senha123 "}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f]{64}")))
            .andReturn().getResponse().getContentAsString();
        return resultado.split("\"")[3];
    }

    @Test
    void cadastroLoginTokenEExpiracao() throws Exception {
        cadastrar();
        String token = entrar();
        var hash = SessaoService.hashBearer("Bearer " + token);
        assertNotEquals(token, hash);
        assertEquals(1, db.sql("SELECT count(*) FROM auth_sessao WHERE token_hash=? AND expira_em=CURRENT_TIMESTAMP + INTERVAL '8 hours'")
            .param(hash).query(Integer.class).single());
        mvc.perform(get("/api/usuaria").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Ana"));
    }

    @Test
    void senhaIncorretaEEspacosPreservados() throws Exception {
        cadastrar();
        for (String senha : new String[]{"errada", "senha123"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.com\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
        }
        assertEquals(0, db.sql("SELECT count(*) FROM auth_sessao").query(Integer.class).single());
    }

    @Test
    void emailInexistente() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"ausente@example.com\",\"senha\":\"senha123\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void tokensInvalidos() throws Exception {
        for (String header : new String[]{"", "Bearer invalido", "bearer " + "a".repeat(64)}) {
            mvc.perform(get("/api/usuaria").header("Authorization", header))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Entre para acessar o CyberTalk."));
            mvc.perform(delete("/api/auth/sessao").header("Authorization", header)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/usuaria").header("Authorization", "Bearer " + "a".repeat(64)))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Sessão expirada."));
    }

    @Test
    void sessaoExpiradaELimpezaNoLogin() throws Exception {
        cadastrar();
        String token = entrar();
        db.sql("UPDATE auth_sessao SET expira_em=CURRENT_TIMESTAMP").update();
        mvc.perform(get("/api/usuaria").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Sessão expirada."));
        entrar();
        assertEquals(0, db.sql("SELECT count(*) FROM auth_sessao WHERE token_hash=?")
            .param(SessaoService.hashBearer("Bearer " + token)).query(Integer.class).single());
    }

    @Test
    void logoutImpedeReutilizacao() throws Exception {
        cadastrar();
        String token = entrar();
        mvc.perform(delete("/api/auth/sessao").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/usuaria").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Sessão expirada."));
        mvc.perform(delete("/api/auth/sessao").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
    }

    @Test
    void exigeParticipacaoNaComunidade() throws Exception {
        cadastrar();
        String auth = "Bearer " + entrar();
        mvc.perform(get("/api/posts").header("Authorization", auth))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("Entre na comunidade primeiro."));
        mvc.perform(put("/api/comunidade/adesao").header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/posts").header("Authorization", auth)).andExpect(status().isOk());
    }

    @Test
    void aceitaHashDaConfiguracaoAnteriorENormalizaEmailNoService() {
        String hash = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(" senha123 ");
        db.sql("INSERT INTO conta(nome,email,senha_hash) VALUES ('Legada','legada@example.com',?)")
            .param(hash).update();
        var service = contexto.getBean(SessaoService.class);
        String token = service.entrar(new LoginRequest("  LEGADA@EXAMPLE.COM  ", " senha123 "));
        assertTrue(token.matches("[0-9a-f]{64}"));
    }

    @Test
    void dtoRejeitaCamposInvalidos() throws Exception {
        for (String corpo : new String[]{"{}", "{\"email\":\"invalido\",\"senha\":\"senha123\"}",
                "{\"email\":\"ana@example.com\",\"senha\":\" \"}"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        }
        assertEquals(0, db.sql("SELECT count(*) FROM auth_sessao").query(Integer.class).single());
    }

    @Test
    void sessaoPreexistenteIdentificaSemExigirParticipacaoEPreservaRespostaDoTalk() throws Exception {
        long id = db.sql("""
            INSERT INTO conta(nome,email,senha_hash)
            VALUES ('Legada','legada@example.com',?) RETURNING id
            """).param(Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(" senha123 "))
            .query(Long.class).single();
        db.sql("INSERT INTO usuaria(conta_id,competencias) VALUES (?,'Java')").param(id).update();
        // Token e hash fixos apenas da fixture: independem do gerador e da função de hash atuais.
        String auth = "Bearer " + "a".repeat(64);
        String hash = "ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb";
        db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP + INTERVAL '2 hours')")
            .params(hash, id).update();
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", auth);
        var acesso = new AcessoAutenticado(new SessaoRepository(db), request, new PapelRepository(db));
        assertEquals(id, acesso.identidade().id());
        mvc.perform(get("/api/usuaria").header("Authorization", auth)).andExpect(status().isOk())
            .andExpect(content().json("""
                {"id":%d,"nome":"Legada","competencias":"Java","membro":false}
                """.formatted(id), JsonCompareMode.STRICT));
        mvc.perform(get("/api/posts").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(put("/api/comunidade/adesao").header("Authorization", auth)).andExpect(status().isNoContent());
        assertEquals(id, acesso.identidade().id());
        mvc.perform(get("/api/usuaria").header("Authorization", auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.membro").value(true));
        mvc.perform(get("/api/posts").header("Authorization", auth)).andExpect(status().isOk());
        assertEquals(1, db.sql("SELECT count(*) FROM auth_sessao WHERE token_hash=? AND expira_em=CURRENT_TIMESTAMP + INTERVAL '2 hours'")
            .param(hash).query(Integer.class).single());
        mvc.perform(delete("/api/auth/sessao").header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/usuaria").header("Authorization", auth)).andExpect(status().isUnauthorized());
    }
}
