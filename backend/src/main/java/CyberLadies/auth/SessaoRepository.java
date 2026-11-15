package CyberLadies.auth;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class SessaoRepository {
    private final JdbcClient db;

    SessaoRepository(JdbcClient db) { this.db = db; }

    void removerExpiradas() {
        db.sql("DELETE FROM auth_sessao WHERE expira_em <= CURRENT_TIMESTAMP").update();
    }

    void inserir(String tokenHash, long usuariaId) {
        db.sql("INSERT INTO auth_sessao (token_hash,usuaria_id,expira_em) VALUES (?,?,CURRENT_TIMESTAMP + INTERVAL '8 hours')")
            .params(tokenHash, usuariaId).update();
    }

    void remover(String tokenHash) {
        db.sql("DELETE FROM auth_sessao WHERE token_hash=?").param(tokenHash).update();
    }

    Optional<Long> buscarConta(String tokenHash) {
        return db.sql("""
            SELECT u.id FROM conta u
            JOIN auth_sessao s ON s.usuaria_id=u.id
            WHERE s.token_hash=? AND s.expira_em > CURRENT_TIMESTAMP
            """).param(tokenHash).query(Long.class).optional();
    }
}
