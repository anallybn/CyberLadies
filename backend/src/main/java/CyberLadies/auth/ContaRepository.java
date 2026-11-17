package CyberLadies.auth;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class ContaRepository {
    record Credencial(long id, String hash) {}
    private final JdbcClient db;

    ContaRepository(JdbcClient db) { this.db = db; }

    Optional<Credencial> buscarCredencial(String email) {
        return db.sql("SELECT id,senha_hash FROM conta WHERE email=?").param(email)
            .query((rs, n) -> new Credencial(rs.getLong("id"), rs.getString("senha_hash"))).optional();
    }
}
