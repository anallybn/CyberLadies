package CyberLadies.talk;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class AcessoTalk {
    record Usuaria(long id, String nome, String competencias, boolean membro) {}
    private final JdbcClient db;
    private final HttpServletRequest request;

    AcessoTalk(JdbcClient db, HttpServletRequest request) {
        this.db = db;
        this.request = request;
    }

    Usuaria usuaria() {
        return db.sql("""
            SELECT u.id,u.nome,u.competencias,u.membro FROM talk_usuaria u
            JOIN talk_sessao s ON s.usuaria_id=u.id
            WHERE s.token_hash=? AND s.expira_em > CURRENT_TIMESTAMP
            """).param(ContaApi.hashBearer(request.getHeader("Authorization")))
            .query(Usuaria.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada."));
    }

    Usuaria membro() {
        var pessoa = usuaria();
        if (!pessoa.membro()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Entre na comunidade primeiro.");
        return pessoa;
    }
}
