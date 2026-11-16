package CyberLadies.academy.repository;
import CyberLadies.academy.dto.AlunasDados.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AlunasRepository {
    private final JdbcClient db;
    public AlunasRepository(JdbcClient db) { this.db=db; }
    private static final String BASE="""
        WITH contagens AS (
          SELECT i.usuaria_id aluna_id,t.id trilha_id,u.nome,t.titulo trilha,i.criado_em inscrita_em,
           (SELECT count(*) FROM academy_aula a WHERE a.trilha_id=t.id) total_aulas,
           (SELECT count(*) FROM academy_conclusao c WHERE c.usuaria_id=i.usuaria_id AND c.trilha_id=t.id) aulas_concluidas
          FROM academy_autoria p JOIN academy_trilha t ON t.id=p.trilha_id
          JOIN academy_inscricao i ON i.trilha_id=t.id JOIN talk_usuaria u ON u.id=i.usuaria_id
          WHERE p.educadora_id=:dona
        ), progresso AS (
          SELECT *,CASE WHEN total_aulas>0 THEN (100*aulas_concluidas/total_aulas)::integer ELSE 0 END percentual
          FROM contagens
        )
        """;
    public Indicadores indicadores(long dona) {
        return db.sql(BASE+"""
            SELECT count(DISTINCT aluna_id) total_alunas,
             count(*) FILTER(WHERE total_aulas>0 AND aulas_concluidas=total_aulas) inscricoes_concluidas,
             count(*) FILTER(WHERE NOT(total_aulas>0 AND aulas_concluidas=total_aulas)) inscricoes_em_andamento,
             count(*) inscricoes FROM progresso
            """).param("dona",dona).query(Indicadores.class).single();
    }
    public List<Trilha> trilhas(long dona) {
        return db.sql("SELECT t.id,t.titulo FROM academy_autoria p JOIN academy_trilha t ON t.id=p.trilha_id WHERE p.educadora_id=? ORDER BY t.titulo,t.id")
            .param(dona).query(Trilha.class).list();
    }
    public boolean propria(long dona,long trilha) {
        return db.sql("SELECT EXISTS(SELECT 1 FROM academy_autoria WHERE educadora_id=? AND trilha_id=?)").params(dona,trilha).query(Boolean.class).single();
    }
    public List<Inscricao> listar(long dona,String busca,String status,Long trilha) {
        String sql=BASE+"SELECT * FROM progresso WHERE strpos(lower(nome),lower(:busca))>0";
        if(trilha!=null)sql+=" AND trilha_id=:trilha";
        if(status.equals("CONCLUIDAS"))sql+=" AND total_aulas>0 AND aulas_concluidas=total_aulas";
        if(status.equals("ANDAMENTO"))sql+=" AND NOT(total_aulas>0 AND aulas_concluidas=total_aulas)";
        var consulta=db.sql(sql+" ORDER BY lower(nome),trilha,trilha_id,aluna_id").param("dona",dona).param("busca",busca);
        if(trilha!=null)consulta.param("trilha",trilha);
        return consulta.query(Inscricao.class).list();
    }
    public Optional<Inscricao> inscricao(long dona,long aluna,long trilha) {
        return db.sql(BASE+"SELECT * FROM progresso WHERE aluna_id=:aluna AND trilha_id=:trilha")
            .param("dona",dona).param("aluna",aluna).param("trilha",trilha).query(Inscricao.class).optional();
    }
    public List<Aula> aulas(long dona,long aluna,long trilha) {
        return db.sql("""
            SELECT a.id,a.titulo,a.ordem,c.aula_id IS NOT NULL concluida,c.concluido_em concluida_em
            FROM academy_autoria p JOIN academy_inscricao i ON i.trilha_id=p.trilha_id AND i.usuaria_id=?
            JOIN academy_aula a ON a.trilha_id=p.trilha_id
            LEFT JOIN academy_conclusao c ON c.aula_id=a.id AND c.trilha_id=a.trilha_id AND c.usuaria_id=i.usuaria_id
            WHERE p.educadora_id=? AND p.trilha_id=? ORDER BY a.ordem,a.id
            """).params(aluna,dona,trilha).query(Aula.class).list();
    }
}
