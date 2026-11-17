package CyberLadies.talk;

import CyberLadies.academy.service.VideoSeguro;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.test.context.jdbc.Sql(scripts="/db/fixture-conta-integracao.sql", executionPhase=org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS, config=@org.springframework.test.context.jdbc.SqlConfig(separator="@@@"))
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcademyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    @Autowired VideoSeguro videos;
    long usuaria,outra,trilha,aula1,aula2,mentora,slot;
    String auth="Bearer "+"e".repeat(64), outraAuth="Bearer "+"f".repeat(64), marcador;
    @BeforeEach void preparar() {
        marcador="academy-fixture-"+UUID.randomUUID();
        usuaria=conta(); outra=conta();
        db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour')")
            .params(CyberLadies.auth.service.Credenciais.hashBearer(auth),usuaria).update();
        db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour')")
            .params(CyberLadies.auth.service.Credenciais.hashBearer(outraAuth),outra).update();
        trilha=db.sql("INSERT INTO academy_trilha(titulo,descricao,categoria,nivel,elegivel_certificado) VALUES (?,?,'FRONTEND','INICIANTE',true) RETURNING id")
            .params(marcador,"Introdução "+marcador).query(Long.class).single();
        aula1=aula(trilha,1,"YOUTUBE","M7lc1UVf-VE");
        aula2=aula(trilha,2,"VIMEO","123456789");
        mentora=db.sql("INSERT INTO academy_mentora(nome,especialidade) VALUES (?,'Front-end') RETURNING id").param(marcador).query(Long.class).single();
        slot=horario(mentora,OffsetDateTime.now().plusDays(2),90);
    }
    long conta() { return db.sql("SELECT fixture_conta_integracao('Fixture',?,'fixture','','CYBERLADIE',false)")
        .param(UUID.randomUUID()+"@example.invalid").query(Long.class).single(); }
    long aula(long trilha,int ordem,String provedor,String video) {
        return db.sql("""
            INSERT INTO academy_aula(trilha_id,titulo,descricao,ordem,duracao_minutos,video_provedor,video_id,uso_autorizado)
            VALUES (?,?,?, ?,30,?,?,true) RETURNING id
            """).params(trilha,marcador+" aula "+ordem,"Introdução "+marcador,ordem,provedor,video).query(Long.class).single();
    }
    long horario(long mentora,OffsetDateTime inicio,int minutos) {
        return db.sql("INSERT INTO academy_disponibilidade(mentora_id,inicio,fim) VALUES (?,?,?) RETURNING id")
            .params(mentora,inicio,inicio.plusMinutes(minutos)).query(Long.class).single();
    }
    void inscrever() throws Exception {
        mvc.perform(post("/api/academy/trilhas/"+trilha+"/inscricoes").header("Authorization",auth)).andExpect(status().isCreated());
    }
    void rejeitarSql(String sql,Object... params) {
        db.sql("SAVEPOINT academy_rejeicao").update();
        assertThatThrownBy(()->db.sql(sql).params(params).update()).isInstanceOf(DataIntegrityViolationException.class);
        db.sql("ROLLBACK TO SAVEPOINT academy_rejeicao").update();
        db.sql("RELEASE SAVEPOINT academy_rejeicao").update();
    }
    @Test void catalogoPesquisaFiltrosEIndicadores() throws Exception {
        mvc.perform(get("/api/academy/catalogo").param("busca",marcador).param("categoria","FRONTEND").param("nivel","INICIANTE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.trilhas.length()").value(1))
            .andExpect(jsonPath("$.aulas.length()").value(2)).andExpect(jsonPath("$.indicadores.percentualGratuito").value(100));
        mvc.perform(get("/api/academy/trilhas").param("busca",marcador).param("categoria","DADOS"))
            .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/academy/trilhas").param("busca","INTRODUCAO "+marcador)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/academy/trilhas/"+trilha)).andExpect(status().isOk()).andExpect(jsonPath("$.cargaMinutos").value(60));
        mvc.perform(get("/api/academy/trilhas/"+trilha+"/aulas")).andExpect(jsonPath("$[0].id").value(aula1)).andExpect(jsonPath("$[1].id").value(aula2));
        mvc.perform(get("/api/academy/catalogo").param("categoria","INVALIDA")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/academy/catalogo").param("nivel","INVALIDO")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/academy/catalogo").param("busca","x".repeat(151))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/academy/trilhas/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/academy/trilhas/9223372036854775807")).andExpect(status().isNotFound());
    }
    @Test void videosSomenteDeProvedoresEIdentificadoresAutorizados() throws Exception {
        mvc.perform(get("/api/academy/aulas/"+aula1)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/academy/aulas")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].videoId").doesNotExist()).andExpect(jsonPath("$[0].complementar").doesNotExist());
        mvc.perform(get("/api/academy/aulas/"+aula1).header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.video.url").value("https://www.youtube-nocookie.com/embed/M7lc1UVf-VE"))
            .andExpect(jsonPath("$.proximaAulaId").value(aula2));
        mvc.perform(get("/api/academy/aulas/"+aula2).header("Authorization",auth)).andExpect(jsonPath("$.video.url").value("https://player.vimeo.com/video/123456789"));
        for (String id : new String[]{"https://evil.invalid","../video","<script>","javascript:1","a?origin=x"})
            assertThatThrownBy(()->videos.gerar("YOUTUBE",id)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->videos.gerar("OUTRO","M7lc1UVf-VE")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        rejeitarSql("UPDATE academy_aula SET video_id='javascript:alert(1)' WHERE id=?",aula1);
        rejeitarSql("UPDATE academy_aula SET uso_autorizado=false WHERE id=?",aula1);
        mvc.perform(post("/api/academy/trilhas").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isMethodNotAllowed());
    }
    @Test void inscricaoConclusaoProgressoEIsolamento() throws Exception {
        mvc.perform(post("/api/academy/trilhas/"+trilha+"/inscricoes")).andExpect(status().isUnauthorized());
        inscrever();
        mvc.perform(post("/api/academy/trilhas/"+trilha+"/inscricoes").header("Authorization",auth)).andExpect(status().isConflict());
        mvc.perform(get("/api/academy/me/inscricoes").header("Authorization",auth)).andExpect(jsonPath("$[0].trilhaId").value(trilha));
        mvc.perform(get("/api/academy/trilhas/"+trilha+"/progresso").header("Authorization",auth)).andExpect(jsonPath("$.percentual").value(0));
        for (int i=0;i<2;i++)
            mvc.perform(put("/api/academy/aulas/"+aula1+"/conclusao").header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"usuariaId\":"+outra+"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.percentual").value(50))
                .andExpect(jsonPath("$.proximaAulaId").value(aula2));
        mvc.perform(get("/api/academy/trilhas/"+trilha+"/progresso").header("Authorization",outraAuth)).andExpect(status().isForbidden());
        mvc.perform(put("/api/academy/aulas/"+aula2+"/conclusao").header("Authorization",outraAuth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/academy/me/progresso").header("Authorization",outraAuth)).andExpect(jsonPath("$").isEmpty());
        assertThat(db.sql("SELECT count(*) FROM academy_conclusao WHERE usuaria_id=?").param(usuaria).query(Long.class).single()).isEqualTo(1);
    }
    @Test void trilhaEOrdemImutaveisEVersionamento() throws Exception {
        inscrever();
        rejeitarSql("UPDATE academy_trilha SET titulo='Alterada' WHERE id=?",trilha);
        rejeitarSql("UPDATE academy_trilha SET congelada=false WHERE id=?",trilha);
        rejeitarSql("UPDATE academy_aula SET ordem=3 WHERE id=?",aula1);
        rejeitarSql("DELETE FROM academy_aula WHERE id=?",aula1);
        rejeitarSql("""
            INSERT INTO academy_aula(trilha_id,titulo,descricao,ordem,duracao_minutos,video_provedor,video_id,uso_autorizado)
            VALUES (?,'Nova','',3,10,'YOUTUBE','M7lc1UVf-VE',true)
            """,trilha);
        long versao=db.sql("""
            INSERT INTO academy_trilha(titulo,descricao,categoria,nivel,origem_id,versao)
            SELECT titulo,descricao,categoria,nivel,id,2 FROM academy_trilha WHERE id=? RETURNING id
            """).param(trilha).query(Long.class).single();
        aula(versao,1,"YOUTUBE","M7lc1UVf-VE");
        mvc.perform(get("/api/academy/trilhas/"+trilha)).andExpect(jsonPath("$.versao").value(1)).andExpect(jsonPath("$.totalAulas").value(2));
        mvc.perform(get("/api/academy/trilhas/"+versao)).andExpect(jsonPath("$.versao").value(2));
    }
    @Test void certificadoAutomaticoUnicoSnapshotEIntegracaoPerfil() throws Exception {
        inscrever();
        rejeitarSql("""
            INSERT INTO academy_certificado(usuaria_id,trilha_id,nome_usuaria,titulo_trilha,versao,total_aulas,carga_minutos)
            VALUES (?,?,'Forjado','Forjado',1,1,1)
            """,usuaria,trilha);
        mvc.perform(put("/api/academy/aulas/"+aula1+"/conclusao").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(get("/api/academy/me/certificados").header("Authorization",auth)).andExpect(jsonPath("$").isEmpty());
        for (int i=0;i<2;i++)
            mvc.perform(put("/api/academy/aulas/"+aula2+"/conclusao").header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.percentual").value(100));
        var id=db.sql("SELECT id FROM academy_certificado WHERE usuaria_id=? AND trilha_id=?").params(usuaria,trilha).query(UUID.class).single();
        mvc.perform(get("/api/academy/me/certificados/"+id).header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAulas").value(2)).andExpect(jsonPath("$.cargaMinutos").value(60));
        mvc.perform(get("/api/academy/me/certificados/"+id).header("Authorization",outraAuth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/academy/me/certificados")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/perfil").header("Authorization",auth))
            .andExpect(jsonPath("$.certificadosPlataforma.length()").value(1)).andExpect(jsonPath("$.certificacoes").isEmpty());
        rejeitarSql("UPDATE academy_certificado SET titulo_trilha='Forjado' WHERE id=?",id);
        assertThat(db.sql("SELECT count(*) FROM academy_certificado WHERE usuaria_id=? AND trilha_id=?").params(usuaria,trilha).query(Long.class).single()).isEqualTo(1);
    }
    @Test void trilhaNaoElegivelNaoEmiteCertificado() throws Exception {
        db.sql("UPDATE academy_trilha SET elegivel_certificado=false WHERE id=?").param(trilha).update();
        inscrever();
        for (long aula : new long[]{aula1,aula2})
            mvc.perform(put("/api/academy/aulas/"+aula+"/conclusao").header("Authorization",auth)).andExpect(status().isOk());
        assertThat(db.sql("SELECT count(*) FROM academy_certificado WHERE usuaria_id=?").param(usuaria).query(Long.class).single()).isZero();
    }
    @Test void horariosReservaCancelamentoAutoriaESobreposicao() throws Exception {
        mvc.perform(get("/api/academy/mentoras")).andExpect(status().isOk());
        mvc.perform(get("/api/academy/mentoras/"+mentora+"/horarios")).andExpect(jsonPath("$[0].id").value(slot));
        String corpo="{\"disponibilidadeId\":"+slot+",\"usuariaId\":"+outra+"}";
        mvc.perform(post("/api/academy/agendamentos").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/academy/agendamentos").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("CONFIRMADO"));
        long agendamento=db.sql("SELECT id FROM academy_agendamento WHERE usuaria_id=?").param(usuaria).query(Long.class).single();
        mvc.perform(post("/api/academy/agendamentos").header("Authorization",outraAuth).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isConflict());
        mvc.perform(get("/api/academy/me/agendamentos").header("Authorization",outraAuth)).andExpect(jsonPath("$").isEmpty());
        mvc.perform(delete("/api/academy/agendamentos/"+agendamento).header("Authorization",outraAuth)).andExpect(status().isNotFound());
        long segunda=db.sql("INSERT INTO academy_mentora(nome,especialidade) VALUES ('Fixture segunda','Dados') RETURNING id").query(Long.class).single();
        OffsetDateTime inicio=db.sql("SELECT inicio FROM academy_disponibilidade WHERE id=?").param(slot).query(OffsetDateTime.class).single();
        long sobreposto=horario(segunda,inicio.plusMinutes(45),90);
        mvc.perform(post("/api/academy/agendamentos").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"disponibilidadeId\":"+sobreposto+"}")).andExpect(status().isConflict());
        rejeitarSql("INSERT INTO academy_disponibilidade(mentora_id,inicio,fim) VALUES (?,?,?)",mentora,inicio.plusMinutes(30),inicio.plusHours(2));
        rejeitarSql("UPDATE academy_disponibilidade SET fim=fim+INTERVAL '1 hour' WHERE id=?",slot);
        mvc.perform(delete("/api/academy/agendamentos/"+agendamento).header("Authorization",auth)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/academy/agendamentos/"+agendamento).header("Authorization",auth)).andExpect(status().isNoContent());
        mvc.perform(post("/api/academy/agendamentos").header("Authorization",outraAuth).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated());
    }
    @Test void bloqueioDePassadoESessaoExpirada() throws Exception {
        rejeitarSql("INSERT INTO academy_disponibilidade(mentora_id,inicio,fim) VALUES (?,clock_timestamp()-INTERVAL '1 hour',clock_timestamp())",mentora);
        mvc.perform(post("/api/academy/agendamentos").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"disponibilidadeId\":0}")).andExpect(status().isBadRequest());
        db.sql("UPDATE auth_sessao SET expira_em=CURRENT_TIMESTAMP-INTERVAL '1 hour' WHERE usuaria_id=?").param(usuaria).update();
        mvc.perform(get("/api/academy/me/progresso").header("Authorization",auth)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/academy/me/agendamentos").header("Authorization",auth)).andExpect(status().isUnauthorized());
    }
    @Test void cancelamentoEReservaAposInicioSaoBloqueados() throws Exception {
        long curto=horario(mentora,OffsetDateTime.now().plusSeconds(3),30);
        mvc.perform(post("/api/academy/agendamentos").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"disponibilidadeId\":"+curto+"}")).andExpect(status().isCreated());
        long reserva=db.sql("SELECT id FROM academy_agendamento WHERE disponibilidade_id=?").param(curto).query(Long.class).single();
        Thread.sleep(3200);
        mvc.perform(delete("/api/academy/agendamentos/"+reserva).header("Authorization",auth)).andExpect(status().isConflict());
        rejeitarSql("UPDATE academy_agendamento SET status='CANCELADO' WHERE id=?",reserva);
        mvc.perform(post("/api/academy/agendamentos").header("Authorization",outraAuth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"disponibilidadeId\":"+curto+"}")).andExpect(status().isConflict());
    }
    @Test void horariosAdjacentesNaoSaoSobreposicao() throws Exception {
        OffsetDateTime inicio=db.sql("SELECT fim FROM academy_disponibilidade WHERE id=?").param(slot).query(OffsetDateTime.class).single();
        long adjacente=horario(mentora,inicio,45);
        for(long horario : new long[]{slot,adjacente})
            mvc.perform(post("/api/academy/agendamentos").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"disponibilidadeId\":"+horario+"}")).andExpect(status().isCreated());
    }}