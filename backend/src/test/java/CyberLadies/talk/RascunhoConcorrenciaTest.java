package CyberLadies.talk;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest @AutoConfigureMockMvc
class RascunhoConcorrenciaTest {
    @Autowired MockMvc mvc; @Autowired JdbcClient db;
    long dona; String auth; String caminho="/api/educadora/materiais/rascunhos";
    @BeforeEach void preparar() {
        assertThat(db.sql("SELECT current_database()").query(String.class).single()).isEqualTo("cyberladies_auth_restore_5958ab52de");
        dona=db.sql("INSERT INTO talk_usuaria(nome,email,senha_hash,competencias,tipo) VALUES ('Fixture concorrência privada',?,'fixture','','EDUCADORA') RETURNING id")
            .param(UUID.randomUUID()+"@example.invalid").query(Long.class).single();
        db.sql("INSERT INTO auth_educadora(usuaria_id,especialidade,instituicao) VALUES (?,'Teste','Teste')").param(dona).update();
        auth="Bearer "+UUID.randomUUID().toString().replace("-","").repeat(2);
        db.sql("INSERT INTO talk_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour')").params(ContaApi.hashBearer(auth),dona).update();
    }
    @AfterEach void limpar() {
        db.sql("DELETE FROM academy_material_rascunho WHERE educadora_id=?").param(dona).update();
        db.sql("DELETE FROM talk_sessao WHERE usuaria_id=?").param(dona).update();
        db.sql("DELETE FROM auth_educadora WHERE usuaria_id=?").param(dona).update();
        db.sql("DELETE FROM talk_usuaria WHERE id=?").param(dona).update();
    }
    @Test void duasCriacoesSimultaneasProduzemUmUnicoRegistro() throws Exception {
        String chave=UUID.randomUUID().toString(); var inicio=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Integer> chamada=()->{inicio.await();return mvc.perform(post(caminho).header("Authorization",auth).header("Idempotency-Key",chave)
                .contentType("application/json").content("{\"titulo\":\"Concorrente\"}")).andReturn().getResponse().getStatus();};
            var a=executor.submit(chamada);var b=executor.submit(chamada);inicio.countDown();
            assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(201);assertThat(b.get(20,TimeUnit.SECONDS)).isEqualTo(201);
            assertThat(db.sql("SELECT count(*) FROM academy_material_rascunho WHERE educadora_id=?").param(dona).query(Long.class).single()).isEqualTo(1);
        }
    }
    @Test void duasAbasComMesmaRevisaoAceitamSomenteUmaAtualizacao() throws Exception {
        mvc.perform(post(caminho).header("Authorization",auth).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content("{\"titulo\":\"Original\"}"));
        UUID id=db.sql("SELECT id FROM academy_material_rascunho WHERE educadora_id=?").param(dona).query(UUID.class).single();
        var inicio=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Integer> chamada=()->{inicio.await();return mvc.perform(put(caminho+"/"+id).header("Authorization",auth).header("Idempotency-Key",UUID.randomUUID())
                .contentType("application/json").content("{\"titulo\":\"Edição\",\"revisao\":1}")).andReturn().getResponse().getStatus();};
            var a=executor.submit(chamada);var b=executor.submit(chamada);inicio.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
            assertThat(db.sql("SELECT revisao FROM academy_material_rascunho WHERE id=?").param(id).query(Long.class).single()).isEqualTo(2);
        }
    }
}
