package CyberLadies.educadora;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class AnaliseRepository {
    private static final String SELECT = """
        SELECT e.conta_id,c.nome,e.especialidade,e.instituicao,e.status,
               e.motivo_rejeicao,e.decidida_por,e.decidida_em
        FROM educadora e JOIN conta c ON c.id=e.conta_id
        """;
    private final JdbcClient db;

    AnaliseRepository(JdbcClient db) { this.db = db; }

    List<SolicitacaoAdminResponse> listar(StatusSolicitacao status, int tamanho, long offset) {
        return db.sql(SELECT + " WHERE (:status IS NULL OR e.status=:status) ORDER BY e.conta_id LIMIT :limite OFFSET :offset")
            .param("status", status == null ? null : status.name(), java.sql.Types.VARCHAR)
            .param("limite",tamanho).param("offset",offset).query(SolicitacaoAdminResponse.class).list();
    }

    long contar(StatusSolicitacao status) {
        return db.sql("SELECT count(*) FROM educadora WHERE (:status IS NULL OR status=:status)")
            .param("status",status == null ? null : status.name(),java.sql.Types.VARCHAR).query(Long.class).single();
    }

    Optional<SolicitacaoAdminResponse> buscar(long id) {
        return db.sql(SELECT + " WHERE e.conta_id=?").param(id).query(SolicitacaoAdminResponse.class).optional();
    }

    boolean decidir(long id, String status, String motivo, long administrador) {
        return db.sql("""
            UPDATE educadora SET status=:status,motivo_rejeicao=:motivo,
                decidida_por=:administrador,decidida_em=CURRENT_TIMESTAMP
            WHERE conta_id=:id AND status='PENDENTE'
            """).param("status",status).param("motivo",motivo,java.sql.Types.VARCHAR)
            .param("administrador",administrador).param("id",id).update() == 1;
    }
}
