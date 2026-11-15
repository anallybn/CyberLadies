package CyberLadies.usuaria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CadastroFluxoTests {
    private JdbcClient db;
    private JdbcClient.StatementSpec insert;
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        db = mock(JdbcClient.class);
        insert = mock(JdbcClient.StatementSpec.class);
        when(db.sql(anyString())).thenReturn(insert);
        when(insert.params(any(Object[].class))).thenReturn(insert);
        var service = new CadastroService(new UsuariaRepository(db), Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        mvc = MockMvcBuilders.standaloneSetup(new CadastroApi(service)).build();
    }

    @Test
    void cadastraNormalizaEPreservaSenhaCompativelComLogin() throws Exception {
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nome":"  Ana  ","email":"ANA@EXAMPLE.COM","senha":" senha123 ","competencias":" Java "}
                """))
            .andExpect(status().isCreated()).andExpect(content().string(""));

        verify(db).sql(contains("INSERT INTO usuaria(conta_id,competencias)"));
        verify(insert).params(eq("Ana"), eq("ana@example.com"), argThat((Object hash) ->
            hash instanceof String valor && !valor.equals(" senha123 ") &&
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches(" senha123 ", valor)), eq("Java"));
        verify(insert).update();
    }

    @Test
    void competenciasOpcionaisViramTextoVazio() throws Exception {
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nome":"Ana","email":"ana@example.com","senha":"senha123"}
                """))
            .andExpect(status().isCreated());
        verify(insert).params(eq("Ana"), eq("ana@example.com"), anyString(), eq(""));
    }

    @Test
    void emailDuplicadoRetornaConflito() throws Exception {
        when(insert.update()).thenThrow(new DuplicateKeyException("duplicado"));
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nome":"Ana","email":"ana@example.com","senha":"senha123"}
                """))
            .andExpect(status().isConflict())
            .andExpect(result -> assertEquals("E-mail já cadastrado.",
                ((org.springframework.web.server.ResponseStatusException) result.getResolvedException()).getReason()));
    }

    @Test
    void rejeitaCamposInvalidosAntesDePersistir() throws Exception {
        for (String corpo : new String[] {
            """
                {"nome":" ","email":"ana@example.com","senha":"senha123"}
                """,
            "{\"nome\":\"Ana\",\"email\":\"invalido\",\"senha\":\"senha123\"}",
            "{\"nome\":\"Ana\",\"email\":\"ana@example.com\",\"senha\":\"curta\"}"
        }) {
            mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(db);
    }
}
