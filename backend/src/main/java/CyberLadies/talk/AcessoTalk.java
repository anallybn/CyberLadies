package CyberLadies.talk;

import CyberLadies.auth.service.AcessoConta;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcessoTalk {
    record Usuaria(long id,String nome,String competencias,boolean membro) {}
    private final JdbcClient db;
    private final AcessoConta acesso;
    AcessoTalk(JdbcClient db,AcessoConta acesso) { this.db=db; this.acesso=acesso; }
    Usuaria usuaria() {
        return db.sql("SELECT id,nome,competencias,membro FROM talk_usuaria WHERE id=?")
            .param(estudanteId()).query(Usuaria.class).single();
    }
    public long estudanteId() { return acesso.estudante().id(); }
    public long usuariaId() { return acesso.cyberLadie().id(); }
    Usuaria membro() {
        var pessoa=usuaria();
        if(!pessoa.membro()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Entre na comunidade primeiro.");
        return pessoa;
    }
}