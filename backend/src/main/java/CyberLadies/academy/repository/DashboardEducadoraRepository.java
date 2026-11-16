package CyberLadies.academy.repository;

import CyberLadies.academy.dto.DashboardEducadoraDados.*;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardEducadoraRepository {
    private final JdbcClient db;
    public DashboardEducadoraRepository(JdbcClient db) { this.db=db; }
    public Indicadores indicadores(long educadora) {
        return db.sql("""
            WITH minhas AS (SELECT trilha_id FROM academy_autoria WHERE educadora_id=?),
            totais AS (SELECT a.trilha_id,count(*) total FROM academy_aula a JOIN minhas m ON m.trilha_id=a.trilha_id GROUP BY a.trilha_id)
            SELECT
             (SELECT count(*) FROM academy_aula a JOIN minhas m ON m.trilha_id=a.trilha_id) materiais_publicados,
             (SELECT count(DISTINCT i.usuaria_id) FROM academy_inscricao i JOIN minhas m ON m.trilha_id=i.trilha_id) alunas_alcancadas,
             (SELECT count(*) FROM (
               SELECT i.usuaria_id,i.trilha_id FROM academy_inscricao i JOIN totais t ON t.trilha_id=i.trilha_id
               LEFT JOIN academy_conclusao c ON c.usuaria_id=i.usuaria_id AND c.trilha_id=i.trilha_id
               GROUP BY i.usuaria_id,i.trilha_id,t.total HAVING count(c.aula_id)=t.total
             ) completas) conclusoes
            """).param(educadora).query(Indicadores.class).single();
    }
    public List<Trilha> trilhas(long educadora) {
        return db.sql("""
            SELECT t.id,t.titulo,count(i.usuaria_id) inscricoes FROM academy_autoria p
            JOIN academy_trilha t ON t.id=p.trilha_id
            LEFT JOIN academy_inscricao i ON i.trilha_id=t.id
            WHERE p.educadora_id=? GROUP BY t.id,t.titulo ORDER BY inscricoes DESC,t.titulo,t.id
            """).param(educadora).query(Trilha.class).list();
    }
    public List<Mentoria> mentorias(long educadora) {
        return db.sql("""
            SELECT a.id,u.nome aluna,coalesce(d.tema,'Tema não informado') tema,d.inicio,d.fim
            FROM academy_agendamento a JOIN academy_disponibilidade d ON d.id=a.disponibilidade_id
            JOIN academy_mentora m ON m.id=d.mentora_id JOIN conta u ON u.id=a.usuaria_id
            WHERE m.usuaria_id=? AND a.status='CONFIRMADO' AND d.inicio>clock_timestamp()
            ORDER BY d.inicio,a.id LIMIT 5
            """).param(educadora).query(Mentoria.class).list();
    }
    public List<Material> materiais(long educadora) {
        return db.sql("""
            SELECT a.id,a.titulo,t.titulo trilha,
             (SELECT count(*) FROM academy_aula x WHERE x.trilha_id=t.id) aulas,'Publicado' status
            FROM academy_autoria p JOIN academy_trilha t ON t.id=p.trilha_id
            JOIN academy_aula a ON a.trilha_id=t.id WHERE p.educadora_id=?
            ORDER BY a.criado_em DESC,a.id DESC LIMIT 5
            """).param(educadora).query(Material.class).list();
    }
}
