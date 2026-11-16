package CyberLadies.candidatura.repository;
import CyberLadies.candidatura.model.Candidatura;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository
public class CandidaturaRepository {
    private final JdbcClient db;
    public CandidaturaRepository(JdbcClient db) { this.db = db; }
    public List<Candidatura> listar(long usuaria) {
        return db.sql("SELECT * FROM cyber_candidatura WHERE usuaria_id=? ORDER BY criado_em DESC,id DESC")
            .param(usuaria).query(Candidatura.class).list();
    }
    public Optional<Candidatura> buscar(long id, long usuaria) {
        return db.sql("SELECT * FROM cyber_candidatura WHERE id=? AND usuaria_id=?")
            .params(id,usuaria).query(Candidatura.class).optional();
    }
    public Optional<Candidatura> cadastrar(long usuaria, long vaga) {
        // Trava a vaga até o commit, impedindo encerramento concorrente.
        return db.sql("""
            WITH aberta AS (SELECT id FROM cyber_vaga WHERE id=? AND ativa=true FOR SHARE)
            INSERT INTO cyber_candidatura(usuaria_id,vaga_id) SELECT ?,id FROM aberta
            ON CONFLICT (usuaria_id,vaga_id) DO NOTHING RETURNING *
            """).params(vaga,usuaria).query(Candidatura.class).optional();
    }
}