package CyberLadies.auth.service;

import CyberLadies.auth.AcessoAutenticado;
import CyberLadies.auth.Papel;
import CyberLadies.auth.dto.ContaDados.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Adaptador das telas: identidade e papéis continuam no auth atual. */
@Component
public class AcessoConta {
    private final AcessoAutenticado acesso;
    private final JdbcClient db;
    public AcessoConta(AcessoAutenticado acesso, JdbcClient db) { this.acesso=acesso; this.db=db; }
    public Conta conta() {
        var identidade=acesso.identidade();
        Tipo tipo=identidade.possui(Papel.ADMIN)?Tipo.ADMIN:
            identidade.possui(Papel.EMPRESA)?Tipo.EMPRESA:
            identidade.possui(Papel.EDUCADORA)?Tipo.EDUCADORA:Tipo.CYBERLADIE;
        return db.sql("SELECT c.id,c.nome,c.email,e.status AS status_educadora FROM conta c LEFT JOIN educadora e ON e.conta_id=c.id WHERE c.id=?")
            .param(identidade.id()).query((rs,n)->new Conta(rs.getLong("id"),rs.getString("nome"),rs.getString("email"),tipo,rs.getString("status_educadora"))).single();
    }
    public Conta cyberLadie() {
        var identidade=acesso.identidade();
        if(!identidade.possui(Papel.USUARIA)||identidade.possui(Papel.EDUCADORA))
            throw negado("Candidaturas são exclusivas de usuárias sem papel EDUCADORA.");
        return conta();
    }
    public Conta estudante() {
        if(!acesso.identidade().possui(Papel.USUARIA)) throw negado("Esta conta não possui acesso de estudante.");
        return conta();
    }
    public Conta educadora() {
        if(!acesso.identidade().possui(Papel.EDUCADORA)) throw negado("Acesso exclusivo da Educadora.");
        var conta=conta();
        if(!"APROVADA".equals(conta.statusEducadora())) throw negado("A área privada está disponível somente para educadoras aprovadas nesta integração.");
        return conta;
    }
    private ResponseStatusException negado(String mensagem) { return new ResponseStatusException(HttpStatus.FORBIDDEN,mensagem); }
}
