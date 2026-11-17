package CyberLadies.cyberTalk;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import CyberLadies.auth.AcessoAutenticado;
import CyberLadies.usuaria.ConsultaUsuaria;

@Component
class AcessoTalk {
    record Usuaria(long id, String nome, String competencias, boolean membro) {}
    private final JdbcClient db;
    private final AcessoAutenticado autenticacao;
    private final ConsultaUsuaria usuarias;

    AcessoTalk(JdbcClient db, AcessoAutenticado autenticacao, ConsultaUsuaria usuarias) {
        this.db = db;
        this.autenticacao = autenticacao;
        this.usuarias = usuarias;
    }

    Usuaria usuaria() {
        long id = autenticacao.identidade().id();
        var dados = usuarias.buscar(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada."));
        boolean membro = db.sql("SELECT EXISTS(SELECT 1 FROM talk_participacao WHERE conta_id=?)").param(id)
            .query(Boolean.class).single();
        return new Usuaria(dados.id(), dados.nome(), dados.competencias(), membro);
    }

    Usuaria membro() {
        var pessoa = usuaria();
        if (!pessoa.membro()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Entre na comunidade primeiro.");
        return pessoa;
    }
}
