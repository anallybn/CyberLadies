package CyberLadies.talk;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Fixtures com commit para transações concorrentes: somente em banco de teste explícito.
@SpringBootTest
class AcademyConcurrencyTest {
    @Autowired JdbcClient db;
    @Autowired PlatformTransactionManager manager;
    @BeforeEach void bancoIsolado() {
        assumeTrue(db.sql("SELECT current_database()").query(String.class).single().matches("cyberladies_(academy|auth)_[a-z0-9_]+"),
            "Concorrência requer banco isolado cyberladies_academy_* ou cyberladies_auth_*; nenhuma fixture será criada no principal.");
    }
    long conta() {
        return db.sql("INSERT INTO talk_usuaria(nome,email,senha_hash,competencias) VALUES ('Fixture concorrência',?,'fixture','') RETURNING id")
            .param(UUID.randomUUID()+"@example.invalid").query(Long.class).single();
    }
    long mentora() {
        return db.sql("INSERT INTO academy_mentora(nome,especialidade) VALUES ('Fixture concorrência','Testes') RETURNING id")
            .query(Long.class).single();
    }
    long horario(long mentora,OffsetDateTime inicio,int minutos) {
        return db.sql("INSERT INTO academy_disponibilidade(mentora_id,inicio,fim) VALUES (?,?,?) RETURNING id")
            .params(mentora,inicio,inicio.plusMinutes(minutos)).query(Long.class).single();
    }
    List<Boolean> disputar(Runnable primeira,Runnable segunda) throws Exception {
        var disparo=new CountDownLatch(1);
        Callable<Boolean> a=()->executar(disparo,primeira);
        Callable<Boolean> b=()->executar(disparo,segunda);
        try (var executor=Executors.newFixedThreadPool(2)) {
            var r1=executor.submit(a);
            var r2=executor.submit(b);
            disparo.countDown();
            return List.of(r1.get(15,TimeUnit.SECONDS),r2.get(15,TimeUnit.SECONDS));
        }
    }
    boolean executar(CountDownLatch disparo,Runnable acao) throws Exception {
        disparo.await();
        try {
            new TransactionTemplate(manager).executeWithoutResult(status->acao.run());
            return true;
        } catch (DataIntegrityViolationException esperado) { return false; }
    }
    void reservar(long usuaria,long slot) {
        db.sql("INSERT INTO academy_agendamento(usuaria_id,disponibilidade_id) VALUES (?,?)").params(usuaria,slot).update();
    }
    @Test void duasUsuariasNaoReservamOMesmoHorarioSimultaneamente() throws Exception {
        long m=mentora(),u1=conta(),u2=conta(),h=horario(m,OffsetDateTime.now().plusDays(3),90);
        assertThat(disputar(()->reservar(u1,h),()->reservar(u2,h))).containsExactlyInAnyOrder(true,false);
        assertThat(db.sql("SELECT count(*) FROM academy_agendamento WHERE disponibilidade_id=? AND status='CONFIRMADO'")
            .param(h).query(Long.class).single()).isEqualTo(1);
    }
    @Test void mesmaUsuariaNaoReservaPeriodosSobrepostosComMentorasDiferentes() throws Exception {
        long u=conta(),m1=mentora(),m2=mentora();
        var inicio=OffsetDateTime.now().plusDays(4);
        long h1=horario(m1,inicio,90),h2=horario(m2,inicio.plusMinutes(45),90);
        assertThat(disputar(()->reservar(u,h1),()->reservar(u,h2))).containsExactlyInAnyOrder(true,false);
    }
    @Test void mesmaMentoraNaoRecebeDisponibilidadesSobrepostasConcorrentes() throws Exception {
        long m=mentora();
        var inicio=OffsetDateTime.now().plusDays(5);
        assertThat(disputar(()->horario(m,inicio,90),()->horario(m,inicio.plusMinutes(45),90)))
            .containsExactlyInAnyOrder(true,false);
    }
    @Test void conclusoesSimultaneasEmitemUmUnicoCertificado() throws Exception {
        long u=conta();
        long trilha=db.sql("""
            INSERT INTO academy_trilha(titulo,descricao,categoria,nivel,elegivel_certificado)
            VALUES ('Fixture concorrência','Somente teste','DADOS','INICIANTE',true) RETURNING id
            """).query(Long.class).single();
        long a1=aula(trilha,1),a2=aula(trilha,2);
        db.sql("INSERT INTO academy_inscricao(usuaria_id,trilha_id) VALUES (?,?)").params(u,trilha).update();
        Runnable concluir1=()->db.sql("INSERT INTO academy_conclusao(usuaria_id,trilha_id,aula_id) VALUES (?,?,?) ON CONFLICT DO NOTHING").params(u,trilha,a1).update();
        Runnable concluir2=()->db.sql("INSERT INTO academy_conclusao(usuaria_id,trilha_id,aula_id) VALUES (?,?,?) ON CONFLICT DO NOTHING").params(u,trilha,a2).update();
        assertThat(disputar(concluir1,concluir2)).containsOnly(true);
        assertThat(db.sql("SELECT count(*) FROM academy_conclusao WHERE usuaria_id=? AND trilha_id=?")
            .params(u,trilha).query(Long.class).single()).isEqualTo(2);
        assertThat(db.sql("SELECT count(*) FROM academy_certificado WHERE usuaria_id=? AND trilha_id=?")
            .params(u,trilha).query(Long.class).single()).isEqualTo(1);
        assertThat(disputar(concluir1,concluir2)).containsOnly(true);
        assertThat(db.sql("SELECT count(*) FROM academy_certificado WHERE usuaria_id=? AND trilha_id=?")
            .params(u,trilha).query(Long.class).single()).isEqualTo(1);
    }
    long aula(long trilha,int ordem) {
        return db.sql("""
            INSERT INTO academy_aula(trilha_id,titulo,descricao,ordem,duracao_minutos,video_provedor,video_id,uso_autorizado)
            VALUES (?,'Fixture aula','Somente teste',?,15,'YOUTUBE','M7lc1UVf-VE',true) RETURNING id
            """).params(trilha,ordem).query(Long.class).single();
    }
}