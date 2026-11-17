package CyberLadies.auth;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PapelRepository {
    private final JdbcClient db;

    PapelRepository(JdbcClient db) { this.db = db; }

    Set<Papel> buscar(long contaId) {
        var papeis = EnumSet.noneOf(Papel.class);
        db.sql("SELECT papel FROM conta_papel WHERE conta_id=?").param(contaId)
            .query(String.class).list().forEach(papel -> papeis.add(Papel.valueOf(papel)));
        return papeis;
    }
}
