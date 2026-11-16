package CyberLadies.academy.repository;

import CyberLadies.academy.dto.AcademyDados.*;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AcademyRepository {
    private final JdbcClient db;
    public AcademyRepository(JdbcClient db) { this.db=db; }
    private static final String TRILHAS="""
        SELECT t.id,t.titulo,t.descricao,t.categoria,t.nivel,t.versao,t.elegivel_certificado,
        (SELECT count(*) FROM academy_aula a WHERE a.trilha_id=t.id) AS total_aulas,
        (SELECT COALESCE(sum(duracao_minutos),0) FROM academy_aula a WHERE a.trilha_id=t.id) AS carga_minutos
        FROM academy_trilha t
        """;
    private static final String AULAS="""
        SELECT a.id,a.trilha_id,a.titulo,a.descricao,a.complementar,a.ordem,a.duracao_minutos,
        a.video_provedor,a.video_id,t.categoria,t.nivel
        FROM academy_aula a JOIN academy_trilha t ON t.id=a.trilha_id
        """;
    public List<Trilha> trilhas() { return db.sql(TRILHAS+" ORDER BY t.criado_em DESC,t.id DESC").query(Trilha.class).list(); }
    public Optional<Trilha> trilha(long id) { return db.sql(TRILHAS+" WHERE t.id=?").param(id).query(Trilha.class).optional(); }
    public List<Aula> aulas() { return db.sql(AULAS+" ORDER BY a.criado_em DESC,a.id DESC").query(Aula.class).list(); }
    public List<Aula> aulas(long trilha) { return db.sql(AULAS+" WHERE t.id=? ORDER BY a.ordem,a.id").param(trilha).query(Aula.class).list(); }
    public Optional<Aula> aula(long id) { return db.sql(AULAS+" WHERE a.id=?").param(id).query(Aula.class).optional(); }
    public List<Inscricao> inscricoes(long usuaria) {
        return db.sql("SELECT trilha_id,criado_em FROM academy_inscricao WHERE usuaria_id=? ORDER BY criado_em DESC")
            .param(usuaria).query(Inscricao.class).list();
    }
    public boolean inscrita(long usuaria,long trilha) {
        return db.sql("SELECT EXISTS(SELECT 1 FROM academy_inscricao WHERE usuaria_id=? AND trilha_id=?)")
            .params(usuaria,trilha).query(Boolean.class).single();
    }
    public void bloquearTrilha(long trilha) { db.sql("SELECT id FROM academy_trilha WHERE id=? FOR UPDATE").param(trilha).query(Long.class).single(); }
    public boolean inscrever(long usuaria,long trilha) {
        return db.sql("INSERT INTO academy_inscricao(usuaria_id,trilha_id) VALUES (?,?) ON CONFLICT DO NOTHING").params(usuaria,trilha).update()==1;
    }
    public void bloquearInscricao(long usuaria,long trilha) {
        db.sql("SELECT trilha_id FROM academy_inscricao WHERE usuaria_id=? AND trilha_id=? FOR UPDATE")
            .params(usuaria,trilha).query(Long.class).single();
    }
    public List<Long> concluidas(long usuaria,long trilha) {
        return db.sql("SELECT aula_id FROM academy_conclusao WHERE usuaria_id=? AND trilha_id=? ORDER BY aula_id")
            .params(usuaria,trilha).query(Long.class).list();
    }
    public void concluir(long usuaria,Aula aula) {
        db.sql("INSERT INTO academy_conclusao(usuaria_id,trilha_id,aula_id) VALUES (?,?,?) ON CONFLICT DO NOTHING")
            .params(usuaria,aula.trilhaId(),aula.id()).update();
    }
    private static final String AGENDAMENTOS="""
        SELECT a.id,a.disponibilidade_id,d.mentora_id,m.nome AS mentora,d.inicio,d.fim,a.status
        FROM academy_agendamento a JOIN academy_disponibilidade d ON d.id=a.disponibilidade_id
        JOIN academy_mentora m ON m.id=d.mentora_id WHERE a.usuaria_id=?
        """;
    public List<Mentora> mentoras() {
        return db.sql("""
            SELECT m.id,m.nome,m.especialidade,m.descricao,m.anos_experiencia,
            (SELECT count(*) FROM academy_disponibilidade d WHERE d.mentora_id=m.id AND d.inicio>clock_timestamp()
             AND NOT EXISTS(SELECT 1 FROM academy_agendamento a WHERE a.disponibilidade_id=d.id AND a.status='CONFIRMADO')) AS horarios_livres
            FROM academy_mentora m ORDER BY m.nome,m.id
            """).query(Mentora.class).list();
    }
    public List<Horario> horarios(long mentora) {
        return db.sql("""
            SELECT d.id,d.mentora_id,d.inicio,d.fim FROM academy_disponibilidade d
            WHERE d.mentora_id=? AND d.inicio>clock_timestamp() AND NOT EXISTS(
             SELECT 1 FROM academy_agendamento a WHERE a.disponibilidade_id=d.id AND a.status='CONFIRMADO')
            ORDER BY d.inicio,d.id
            """).param(mentora).query(Horario.class).list();
    }
    public Optional<Horario> horario(long id) {
        return db.sql("SELECT id,mentora_id,inicio,fim FROM academy_disponibilidade WHERE id=?")
            .param(id).query(Horario.class).optional();
    }
    public void bloquearReserva(long usuaria,long mentora,long horario) {
        db.sql("SELECT id FROM talk_usuaria WHERE id=? FOR UPDATE").param(usuaria).query(Long.class).single();
        db.sql("SELECT id FROM academy_mentora WHERE id=? FOR UPDATE").param(mentora).query(Long.class).single();
        db.sql("SELECT id FROM academy_disponibilidade WHERE id=? FOR UPDATE").param(horario).query(Long.class).single();
    }
    public boolean conflito(long usuaria,Horario horario) {
        return db.sql("""
            SELECT EXISTS(SELECT 1 FROM academy_agendamento a JOIN academy_disponibilidade d ON d.id=a.disponibilidade_id
             WHERE a.status='CONFIRMADO' AND d.inicio<? AND d.fim>? AND (a.usuaria_id=? OR d.mentora_id=?))
            """).params(horario.fim(),horario.inicio(),usuaria,horario.mentoraId()).query(Boolean.class).single();
    }
    public long reservar(long usuaria,long horario) {
        return db.sql("INSERT INTO academy_agendamento(usuaria_id,disponibilidade_id) VALUES (?,?) RETURNING id")
            .params(usuaria,horario).query(Long.class).single();
    }
    public List<Agendamento> agendamentos(long usuaria) {
        return db.sql(AGENDAMENTOS+" ORDER BY d.inicio DESC,a.id DESC").param(usuaria).query(Agendamento.class).list();
    }
    public Optional<Agendamento> agendamento(long usuaria,long id) {
        return db.sql(AGENDAMENTOS+" AND a.id=?").params(usuaria,id).query(Agendamento.class).optional();
    }
    public void cancelar(long usuaria,long id) {
        db.sql("UPDATE academy_agendamento SET status='CANCELADO' WHERE id=? AND usuaria_id=? AND status='CONFIRMADO'")
            .params(id,usuaria).update();
    }
    public List<Certificado> certificados(long usuaria) {
        return db.sql("SELECT id,trilha_id,nome_usuaria,titulo_trilha,versao,total_aulas,carga_minutos,emitido_em FROM academy_certificado WHERE usuaria_id=? ORDER BY emitido_em DESC,id")
            .param(usuaria).query(Certificado.class).list();
    }
}