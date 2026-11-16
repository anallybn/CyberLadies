package CyberLadies.talk;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardEducadoraIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    long educadora,outra,aluna,segunda,trilha,extra,legada,aula1,aula2;
    String auth="Bearer "+"1".repeat(64),outroAuth="Bearer "+"2".repeat(64),alunaAuth="Bearer "+"3".repeat(64);
    @BeforeEach void preparar() {
        String banco=db.sql("SELECT current_database()").query(String.class).single();
        assertThat(banco).matches("^cyberladies_auth_[a-z0-9_]+$");
        educadora=conta("EDUCADORA",auth); outra=conta("EDUCADORA",outroAuth);
        aluna=conta("CYBERLADIE",alunaAuth); segunda=conta("CYBERLADIE",null);
        trilha=trilha("Trilha própria"); extra=trilha("Outra própria"); legada=trilha("Não atribuída");
        aula1=aula(trilha,1); aula2=aula(trilha,2); aula(extra,1); aula(legada,1);
        vincular(trilha,educadora); vincular(extra,educadora);
    }
    long conta(String tipo,String token) {
        long id=db.sql("INSERT INTO talk_usuaria(nome,email,senha_hash,competencias,tipo) VALUES ('Fixture dashboard',?,'fixture','',?) RETURNING id")
            .params(UUID.randomUUID()+"@example.invalid",tipo).query(Long.class).single();
        if(tipo.equals("EDUCADORA"))
            db.sql("INSERT INTO auth_educadora(usuaria_id,especialidade,instituicao) VALUES (?,'Teste','Teste')").param(id).update();
        if(token!=null) db.sql("INSERT INTO talk_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour')")
            .params(ContaApi.hashBearer(token),id).update();
        return id;
    }
    long trilha(String titulo) {
        return db.sql("INSERT INTO academy_trilha(titulo,descricao,categoria,nivel) VALUES (?,'Teste','FRONTEND','INICIANTE') RETURNING id")
            .param(titulo).query(Long.class).single();
    }
    long aula(long trilha,int ordem) {
        // Fixture transacional: identificador usado apenas para validar persistência; nenhum vídeo é reproduzido.
        return db.sql("""
            INSERT INTO academy_aula(trilha_id,titulo,descricao,ordem,duracao_minutos,video_provedor,video_id,uso_autorizado)
            VALUES (?,'Material teste','Teste',?,10,'YOUTUBE','M7lc1UVf-VE',true) RETURNING id
            """).params(trilha,ordem).query(Long.class).single();
    }
    void vincular(long trilha,long autora) {
        db.sql("INSERT INTO academy_autoria(trilha_id,educadora_id) VALUES (?,?)").params(trilha,autora).update();
    }
    void inscrever(long pessoa,long trilha) {
        db.sql("INSERT INTO academy_inscricao(usuaria_id,trilha_id) VALUES (?,?)").params(pessoa,trilha).update();
    }
    @Test void metricasDistintasConclusoesIntegraisEMateriais() throws Exception {
        inscrever(aluna,trilha); inscrever(segunda,trilha); inscrever(aluna,extra); inscrever(segunda,legada);
        db.sql("INSERT INTO academy_conclusao(usuaria_id,trilha_id,aula_id) VALUES (?,?,?),(?,?,?),(?,?,?)")
            .params(aluna,trilha,aula1,aluna,trilha,aula2,segunda,trilha,aula1).update();
        mvc.perform(get("/api/educadora/dashboard").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.indicadores.materiaisPublicados").value(3))
            .andExpect(jsonPath("$.indicadores.alunasAlcancadas").value(2))
            .andExpect(jsonPath("$.indicadores.conclusoes").value(1))
            .andExpect(jsonPath("$.trilhas.length()").value(2))
            .andExpect(jsonPath("$.trilhas[0].inscricoes").value(2))
            .andExpect(jsonPath("$.materiais.length()").value(3))
            .andExpect(jsonPath("$.materiais[0].status").value("Publicado"))
            .andExpect(jsonPath("$.podePublicar").value(false));
    }
    @Test void isolamentoMesmoComIdForjadoETrilhasAntigasSemAutora() throws Exception {
        mvc.perform(get("/api/educadora/dashboard").header("Authorization",outroAuth).param("educadoraId",""+educadora))
            .andExpect(status().isOk()).andExpect(jsonPath("$.indicadores.materiaisPublicados").value(0))
            .andExpect(jsonPath("$.indicadores.alunasAlcancadas").value(0))
            .andExpect(jsonPath("$.indicadores.conclusoes").value(0))
            .andExpect(jsonPath("$.trilhas").isEmpty()).andExpect(jsonPath("$.materiais").isEmpty());
        assertThat(db.sql("SELECT count(*) FROM academy_autoria WHERE trilha_id=?").param(legada).query(Long.class).single()).isZero();
    }
    @Test void autenticacaoPerfilEPendenteSemPublicacaoMesmoAprovada() throws Exception {
        mvc.perform(get("/api/educadora/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/educadora/dashboard").header("Authorization",alunaAuth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/educadora/dashboard").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.statusEducadora").value("PENDENTE"));
        for(String estado:new String[]{"PENDENTE","APROVADA"}) {
            db.sql("UPDATE auth_educadora SET status=? WHERE usuaria_id=?").params(estado,educadora).update();
            mvc.perform(post("/api/educadora/trilhas").header("Authorization",auth)).andExpect(status().isForbidden());
            mvc.perform(put("/api/educadora/aulas/1").header("Authorization",auth)).andExpect(status().isForbidden());
            mvc.perform(get("/api/educadora/dashboard").header("Authorization",auth)).andExpect(jsonPath("$.podePublicar").value(false));
        }
    }
    @Test void somenteMentoriasPropriasConfirmadasFuturasSemEmails() throws Exception {
        for(long autora:new long[]{educadora,outra}) {
            long mentora=db.sql("INSERT INTO academy_mentora(usuaria_id,nome,especialidade) VALUES (?,'Mentora fixture','Teste') RETURNING id")
                .param(autora).query(Long.class).single();
            for(int dia=1;dia<=2;dia++) {
                long slot=db.sql("""
                    INSERT INTO academy_disponibilidade(mentora_id,inicio,fim,tema)
                    VALUES (?,CURRENT_TIMESTAMP+(? * INTERVAL '1 day'),CURRENT_TIMESTAMP+(? * INTERVAL '1 day')+INTERVAL '1 hour','Tema real da fixture') RETURNING id
                    """).params(mentora,dia,dia).query(Long.class).single();
                db.sql("INSERT INTO academy_agendamento(usuaria_id,disponibilidade_id,status) VALUES (?,?,?)")
                    .params(autora==educadora?aluna:segunda,slot,"CONFIRMADO").update();
                if(dia==2) db.sql("UPDATE academy_agendamento SET status='CANCELADO' WHERE disponibilidade_id=?").param(slot).update();
            }
        }
        mvc.perform(get("/api/educadora/dashboard").header("Authorization",auth))
            .andExpect(jsonPath("$.mentorias.length()").value(1))
            .andExpect(jsonPath("$.mentorias[0].tema").value("Tema real da fixture"))
            .andExpect(jsonPath("$.mentorias[0].aluna").value("Fixture dashboard"))
            .andExpect(jsonPath("$.mentorias[0].email").doesNotExist());
    }
    @Test void estudanteEducadoraConcluiSemMudarImutabilidadeNemCandidatar() throws Exception {
        db.sql("UPDATE academy_trilha SET elegivel_certificado=true WHERE id=?").param(trilha).update();
        mvc.perform(post("/api/academy/trilhas/"+trilha+"/inscricoes").header("Authorization",outroAuth)).andExpect(status().isCreated());
        mvc.perform(put("/api/academy/aulas/"+aula1+"/conclusao").header("Authorization",outroAuth)).andExpect(status().isOk());
        mvc.perform(put("/api/academy/aulas/"+aula2+"/conclusao").header("Authorization",outroAuth)).andExpect(status().isOk());
        mvc.perform(put("/api/comunidade/adesao").header("Authorization",outroAuth)).andExpect(status().isNoContent());
        mvc.perform(post("/api/candidaturas").header("Authorization",outroAuth)
            .contentType("application/json").content("{\"vagaId\":1}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/academy/me/certificados").header("Authorization",outroAuth)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        db.sql("SAVEPOINT imutabilidade_dashboard").update();
        assertThatThrownBy(()->db.sql("UPDATE academy_trilha SET titulo='Alterada' WHERE id=?").param(trilha).update())
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        db.sql("ROLLBACK TO SAVEPOINT imutabilidade_dashboard").update();
        db.sql("SAVEPOINT autoria_dashboard").update();
        assertThatThrownBy(()->db.sql("UPDATE academy_autoria SET educadora_id=? WHERE trilha_id=?").params(outra,trilha).update())
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        db.sql("ROLLBACK TO SAVEPOINT autoria_dashboard").update();
    }
}
