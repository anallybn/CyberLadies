package CyberLadies.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class PapeisPostgresTests {
    @Autowired WebApplicationContext contexto;
    @Autowired JdbcClient db;
    private MockMvc mvc;
    private long id;
    private String email;
    private final List<Long> contas = new ArrayList<>();
    private static final String AUTH = "Bearer " + "a".repeat(64);
    private static final String HASH = "ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb";

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        email = UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Ana\",\"email\":\"" + email + "\",\"senha\":\" senha123 \"}"))
            .andExpect(status().isCreated());
        id = db.sql("SELECT id FROM conta WHERE email=?").param(email).query(Long.class).single();
        contas.add(id);
        // Simula uma sessão já existente, sem passar pelo gerador de tokens atual.
        db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP + INTERVAL '8 hours')").params(HASH,id).update();
    }

    @AfterEach
    void limparFixtures() {
        for (long conta : contas) db.sql("DELETE FROM conta WHERE id=?").param(conta).update();
    }

    @Test
    void meExibeSomenteIdEPapeisEReconsultaNoServidorComAMesmaSessao() throws Exception {
        conferirMe("[\"USUARIA\"]");
        atribuir("EDUCADORA");
        conferirMe("[\"USUARIA\",\"EDUCADORA\"]");
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", AUTH);
        var acesso = new AcessoAutenticado(new SessaoRepository(db), request, new PapelRepository(db));
        assertTrue(acesso.identidade().possui(Papel.EDUCADORA));
        assertThrows(UnsupportedOperationException.class, () -> acesso.identidade().papeis().add(Papel.ADMIN));
        db.sql("DELETE FROM conta_papel WHERE conta_id=? AND papel='EDUCADORA'").param(id).update();
        assertFalse(acesso.identidade().possui(Papel.EDUCADORA));
        conferirMe("[\"USUARIA\"]");
        mvc.perform(get("/api/posts").header("Authorization", AUTH)).andExpect(status().isForbidden());
        assertEquals(0, db.sql("SELECT count(*) FROM talk_participacao WHERE conta_id=?").param(id).query(Integer.class).single());
    }

    @Test
    void combinacoesInvalidasSaoRecusadasEmInsertUpdateELote() throws Exception {
        try (var c = conectar()) {
            rejeitar(c, "INSERT INTO conta_papel VALUES (" + id + ",'EMPRESA')", "23514");
            atribuir("EDUCADORA");
            rejeitar(c, "UPDATE conta_papel SET papel='EMPRESA' WHERE conta_id=" + id + " AND papel='EDUCADORA'", "23514");
            rejeitar(c, "INSERT INTO conta_papel VALUES (" + id + ",'DESCONHECIDO')", "23514");
            rejeitar(c, "INSERT INTO conta_papel VALUES (" + id + ",'USUARIA')", "23505");
            db.sql("DELETE FROM conta_papel WHERE conta_id=?").param(id).update();
            atribuir("EMPRESA");
            rejeitar(c, "INSERT INTO conta_papel VALUES (" + id + ",'USUARIA')", "23514");
            rejeitar(c, "INSERT INTO conta_papel VALUES (" + id + ",'EDUCADORA')", "23514");
            db.sql("DELETE FROM conta_papel WHERE conta_id=?").param(id).update();
            rejeitar(c, "INSERT INTO conta_papel VALUES (" + id + ",'USUARIA'),(" + id + ",'EMPRESA')", "23514");
            assertEquals(0, db.sql("SELECT count(*) FROM conta_papel WHERE conta_id=?").param(id).query(Integer.class).single());
        }
    }

    @Test
    void cadastroNaoAceitaElevacaoDePrivilegioNemAdesaoPeloPayload() throws Exception {
        String novoEmail = UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Outra","email":"%s","senha":"senha123",
             "papeis":["ADMIN","EMPRESA","EDUCADORA"],"perfil":"ADMIN","admin":true,
             "membro":true,"aprovada":true}
            """.formatted(novoEmail))).andExpect(status().isCreated());
        long outra = db.sql("SELECT id FROM conta WHERE email=?").param(novoEmail).query(Long.class).single();
        contas.add(outra);
        assertEquals(List.of("USUARIA"), db.sql("SELECT papel FROM conta_papel WHERE conta_id=?").param(outra).query(String.class).list());
        assertEquals(0, db.sql("SELECT count(*) FROM talk_participacao WHERE conta_id=?").param(outra).query(Integer.class).single());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + novoEmail + "\",\"senha\":\"senha123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$.token").isString());
    }

    @Test
    void meRecusaSessaoAusenteInvalidaOuExpirada() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + "b".repeat(64))).andExpect(status().isUnauthorized());
        db.sql("UPDATE auth_sessao SET expira_em=CURRENT_TIMESTAMP WHERE token_hash=?").param(HASH).update();
        mvc.perform(get("/api/auth/me").header("Authorization", AUTH)).andExpect(status().isUnauthorized());
    }

    @Test
    void concorrenciaNaoPermiteMisturarEmpresaEPapelPessoal() throws Exception {
        for (int isolamento : new int[]{Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_REPEATABLE_READ}) {
            db.sql("DELETE FROM conta_papel WHERE conta_id=?").param(id).update();
            try (var primeira = conectar(); var segunda = conectar(); var executor = Executors.newSingleThreadExecutor()) {
                primeira.setTransactionIsolation(isolamento); primeira.setAutoCommit(false);
                segunda.setTransactionIsolation(isolamento); segunda.setAutoCommit(false);
                // Estabelece também o snapshot REPEATABLE READ antes do primeiro commit.
                try (var s = segunda.createStatement(); var r = s.executeQuery("SELECT count(*) FROM conta_papel")) { r.next(); }
                try (var s = primeira.createStatement()) { s.executeUpdate("INSERT INTO conta_papel VALUES (" + id + ",'EMPRESA')"); }
                int pid;
                try (var s = segunda.createStatement(); var r = s.executeQuery("SELECT pg_backend_pid()")) { r.next(); pid=r.getInt(1); }
                var tentativa = executor.submit(() -> {
                    try (var s = segunda.createStatement()) {
                        s.setQueryTimeout(5);
                        s.executeUpdate("INSERT INTO conta_papel VALUES (" + id + ",'USUARIA')");
                        segunda.commit();
                        return "aceita";
                    } catch (SQLException e) { segunda.rollback(); return e.getSQLState(); }
                });
                try {
                    long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                    boolean bloqueada = false;
                    while (System.nanoTime() < limite && !tentativa.isDone()) {
                        bloqueada = db.sql("SELECT coalesce(wait_event_type='Lock',false) FROM pg_stat_activity WHERE pid=?")
                            .param(pid).query(Boolean.class).single();
                        if (bloqueada) break;
                        Thread.sleep(10);
                    }
                    assertTrue(bloqueada, "Atribuições da mesma conta precisam ser serializadas.");
                    primeira.commit();
                    assertTrue(List.of("23514","40001").contains(tentativa.get(6, TimeUnit.SECONDS)));
                } finally { primeira.rollback(); }
            }
            assertEquals(List.of("EMPRESA"), db.sql("SELECT papel FROM conta_papel WHERE conta_id=?").param(id).query(String.class).list());
        }
    }

    private void conferirMe(String papeis) throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", AUTH)).andExpect(status().isOk())
            .andExpect(content().json("{\"id\":" + id + ",\"papeis\":" + papeis + "}", JsonCompareMode.STRICT));
    }

    private void atribuir(String papel) { db.sql("INSERT INTO conta_papel VALUES (?,?)").params(id,papel).update(); }
    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(System.getenv("DATABASE_URL"),System.getenv("DATABASE_USER"),System.getenv("DATABASE_PASSWORD"));
    }
    private void rejeitar(Connection c, String sql, String estado) {
        assertEquals(estado, assertThrows(SQLException.class, () -> { try (var s=c.createStatement()) { s.executeUpdate(sql); } }).getSQLState());
    }
}
