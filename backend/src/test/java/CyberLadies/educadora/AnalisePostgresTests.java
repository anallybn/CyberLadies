package CyberLadies.educadora;

import java.time.OffsetDateTime;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class AnalisePostgresTests {
    @Autowired WebApplicationContext contexto;
    @Autowired JdbcClient db;
    @Autowired PasswordEncoder encoder;
    private MockMvc mvc;
    private final List<Long> contas = new ArrayList<>();
    private long educadora;
    private long outra;
    private long administrador;
    private long outroAdmin;
    private String auth;
    private String authOutroAdmin;
    private String authEducadora;
    private String authOutra;
    private static final String BASE = "/api/admin/educadoras/solicitacoes";

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        var primeira = criarConta(false,"Java"); educadora=primeira.id(); authEducadora=primeira.auth();
        var segunda = criarConta(false,"Python"); outra=segunda.id(); authOutra=segunda.auth();
        var admin = criarConta(true,null); administrador=admin.id(); auth=admin.auth();
        var admin2 = criarConta(true,null); outroAdmin=admin2.id(); authOutroAdmin=admin2.auth();
        mvc.perform(put("/api/comunidade/adesao").header("Authorization",authEducadora)).andExpect(status().isNoContent());
    }

    @AfterEach
    void limpar() {
        for (long id : contas) db.sql("DELETE FROM educadora WHERE conta_id=?").param(id).update();
        for (long id : contas) db.sql("DELETE FROM conta WHERE id=?").param(id).update();
    }

    @Test
    void aprovaRegistraResponsavelDataEPreservaPapeisSessoesETalk() throws Exception {
        var papeis = db.sql("SELECT papel FROM conta_papel WHERE conta_id=? ORDER BY papel").param(educadora).query(String.class).list();
        var sessoes = db.sql("SELECT row_to_json(s)::text FROM auth_sessao s WHERE usuaria_id=? ORDER BY token_hash")
            .param(educadora).query(String.class).list();
        OffsetDateTime antes = agora();
        var resultado = decidir(auth,educadora,"{\"status\":\"APROVADA\"}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"))
            .andExpect(jsonPath("$.decididaPor").value(administrador)).andExpect(jsonPath("$.length()").value(8));
        assertNotNull(resultado.andReturn().getResponse().getContentAsString());
        var data = db.sql("SELECT decidida_em FROM educadora WHERE conta_id=?").param(educadora)
            .query((rs,n) -> rs.getObject(1,OffsetDateTime.class)).single();
        assertFalse(data.isBefore(antes)); assertFalse(data.isAfter(agora()));
        assertEquals(papeis,db.sql("SELECT papel FROM conta_papel WHERE conta_id=? ORDER BY papel").param(educadora).query(String.class).list());
        assertEquals(sessoes,db.sql("SELECT row_to_json(s)::text FROM auth_sessao s WHERE usuaria_id=? ORDER BY token_hash")
            .param(educadora).query(String.class).list());
        mvc.perform(get("/api/educadora/solicitacao").header("Authorization",authEducadora))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"))
            .andExpect(jsonPath("$.decididaEm").exists()).andExpect(jsonPath("$.decididaPor").doesNotExist());
        mvc.perform(get("/api/posts").header("Authorization",authEducadora)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization",authEducadora)).andExpect(status().isOk())
            .andExpect(jsonPath("$.papeis.length()").value(2));
    }

    @Test
    void rejeitaComMotivoEConsultaPropriaNaoExpõePedidoAlheio() throws Exception {
        decidir(auth,educadora,"{\"status\":\"REJEITADA\",\"motivo\":\" Documentação insuficiente. \"}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.motivoRejeicao").value("Documentação insuficiente."))
            .andExpect(jsonPath("$.decididaPor").value(administrador));
        mvc.perform(get("/api/educadora/solicitacao").header("Authorization",authEducadora))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADA"))
            .andExpect(jsonPath("$.motivoRejeicao").value("Documentação insuficiente."));
        mvc.perform(get("/api/educadora/solicitacao").header("Authorization",authOutra).queryParam("conta_id",Long.toString(educadora)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.especialidade").value("Python"))
            .andExpect(jsonPath("$.status").value("PENDENTE")).andExpect(jsonPath("$.motivoRejeicao").doesNotExist());
        mvc.perform(get(BASE + "/" + educadora).header("Authorization",authOutra)).andExpect(status().isForbidden());
    }

    @Test
    void motivoObrigatorioEDadosDeDecisaoNaoPodemSerForjados() throws Exception {
        for (String entrada : new String[]{"{}", "{\"status\":\"PENDENTE\"}","{\"status\":\"INVALIDO\"}",
                "{\"status\":\"REJEITADA\"}","{\"status\":\"REJEITADA\",\"motivo\":\" \"}",
                "{\"status\":\"APROVADA\",\"motivo\":\"qualquer\"}",
                "{\"status\":\"REJEITADA\",\"motivo\":\"" + "x".repeat(1001) + "\"}",
                "{\"status\":\"APROVADA\",\"decididaPor\":" + outroAdmin + "}",
                "{\"status\":\"APROVADA\",\"decididaEm\":\"2020-01-01T00:00:00Z\"}"}) {
            decidir(auth,educadora,entrada).andExpect(status().isBadRequest());
        }
        mvc.perform(get(BASE + "/" + educadora).header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDENTE")).andExpect(jsonPath("$.decididaPor").isEmpty());
    }

    @Test
    void todasAsRotasExigemSessaoEAdminAntesDeConsultarPedidos() throws Exception {
        for (String credencial : List.of("",authEducadora,authOutra)) {
            int codigo = credencial.isEmpty() ? 401 : 403;
            mvc.perform(get(BASE).header("Authorization",credencial)).andExpect(status().is(codigo));
            mvc.perform(get(BASE + "/" + educadora).header("Authorization",credencial)).andExpect(status().is(codigo));
            decidir(credencial,educadora,"{\"status\":\"APROVADA\"}").andExpect(status().is(codigo));
            mvc.perform(get(BASE + "/" + Long.MAX_VALUE).header("Authorization",credencial)).andExpect(status().is(codigo));
        }
        mvc.perform(get(BASE).header("Authorization","Bearer invalido")).andExpect(status().isUnauthorized());
    }

    @Test
    void pedidoInexistenteERetentativaNaoSobrescreveDecisao() throws Exception {
        mvc.perform(get(BASE + "/" + Long.MAX_VALUE).header("Authorization",auth)).andExpect(status().isNotFound());
        decidir(auth,Long.MAX_VALUE,"{\"status\":\"APROVADA\"}").andExpect(status().isNotFound());
        decidir(auth,educadora,"{\"status\":\"APROVADA\"}").andExpect(status().isOk());
        String antes = registro();
        decidir(authOutroAdmin,educadora,"{\"status\":\"REJEITADA\",\"motivo\":\"Nova tentativa\"}")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Solicitação já analisada."));
        assertEquals(antes,registro());
    }

    @Test
    void listaComFiltroPaginacaoEOrdenacaoEstavel() throws Exception {
        mvc.perform(get(BASE).header("Authorization",auth).param("tamanho","1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.pagina").value(0))
            .andExpect(jsonPath("$.itens.length()").value(1)).andExpect(jsonPath("$.itens[0].contaId").value(educadora))
            .andExpect(jsonPath("$.itens[0].length()").value(8));
        mvc.perform(get(BASE).header("Authorization",auth).param("tamanho","1").param("pagina","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].contaId").value(outra));
        decidir(auth,educadora,"{\"status\":\"APROVADA\"}").andExpect(status().isOk());
        mvc.perform(get(BASE).header("Authorization",auth).param("status","PENDENTE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.itens[0].contaId").value(outra));
        mvc.perform(get(BASE).header("Authorization",auth).param("status","REJEITADA"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.itens").isEmpty());
        mvc.perform(get(BASE).header("Authorization",auth).param("pagina","20"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.itens").isEmpty());
        for (String[] parametro : List.of(new String[]{"pagina","-1"},new String[]{"tamanho","0"},new String[]{"tamanho","101"},new String[]{"status","INVALIDO"}))
            mvc.perform(get(BASE).header("Authorization",auth).param(parametro[0],parametro[1])).andExpect(status().isBadRequest());
    }

    @Test
    void duasDecisoesConcorrentesMantemApenasAPrimeira() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var inicio = new CountDownLatch(1);
            var aprovar = executor.submit(() -> { inicio.await(); return decidir(auth,educadora,"{\"status\":\"APROVADA\"}").andReturn().getResponse().getStatus(); });
            var rejeitar = executor.submit(() -> { inicio.await(); return decidir(authOutroAdmin,educadora,"{\"status\":\"REJEITADA\",\"motivo\":\"Recusada\"}").andReturn().getResponse().getStatus(); });
            inicio.countDown();
            int a = aprovar.get(10,TimeUnit.SECONDS); int r = rejeitar.get(10,TimeUnit.SECONDS);
            assertTrue((a==200 && r==409) || (a==409 && r==200));
            mvc.perform(get(BASE + "/" + educadora).header("Authorization",auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(a==200 ? "APROVADA" : "REJEITADA"))
                .andExpect(jsonPath("$.decididaPor").value(a==200 ? administrador : outroAdmin));
            String antes = registro();
            decidir(auth,educadora,"{\"status\":\"APROVADA\"}").andExpect(status().isConflict());
            assertEquals(antes,registro());
        }
    }

    private record Conta(long id, String auth) {}
    private Conta criarConta(boolean admin, String especialidade) throws Exception {
        String email = UUID.randomUUID()+"@example.com";
        String senha = UUID.randomUUID().toString();
        long id;
        if (admin) {
            id = db.sql("INSERT INTO conta(nome,email,senha_hash) VALUES ('Admin de teste',?,?) RETURNING id")
                .params(email,encoder.encode(senha)).query(Long.class).single();
            db.sql("INSERT INTO conta_papel VALUES (?,'ADMIN')").param(id).update();
        } else {
            mvc.perform(post("/api/auth/cadastro/usuaria").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana\",\"email\":\""+email+"\",\"senha\":\""+senha+"\"}"))
                .andExpect(status().isCreated());
            id = db.sql("SELECT id FROM conta WHERE email=?").param(email).query(Long.class).single();
        }
        contas.add(id);
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\""+email+"\",\"senha\":\""+senha+"\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String credencial = "Bearer " + login.split("\"")[3];
        if (!admin) mvc.perform(post("/api/educadora/solicitacao").header("Authorization",credencial).contentType(MediaType.APPLICATION_JSON)
            .content("{\"especialidade\":\""+especialidade+"\",\"instituicao\":\"Escola\"}"))
            .andExpect(status().isCreated());
        return new Conta(id,credencial);
    }

    private org.springframework.test.web.servlet.ResultActions decidir(String credencial, long id, String corpo) throws Exception {
        return mvc.perform(post(BASE + "/" + id + "/decisao").header("Authorization",credencial)
            .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }
    private String registro() { return db.sql("SELECT row_to_json(e)::text FROM educadora e WHERE conta_id=?").param(educadora).query(String.class).single(); }
    private OffsetDateTime agora() { return db.sql("SELECT clock_timestamp()").query((rs,n) -> rs.getObject(1,OffsetDateTime.class)).single(); }
}
