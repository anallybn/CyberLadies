package CyberLadies.educadora;

import CyberLadies.auth.AcessoAutenticado;
import CyberLadies.auth.Papel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class AnaliseService {
    private final AcessoAutenticado acesso;
    private final AnaliseRepository solicitacoes;

    AnaliseService(AcessoAutenticado acesso, AnaliseRepository solicitacoes) {
        this.acesso = acesso;
        this.solicitacoes = solicitacoes;
    }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public PaginaSolicitacoes listar(StatusSolicitacao status, int pagina, int tamanho) {
        exigirAdmin();
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe página a partir de zero e tamanho entre 1 e 100.");
        return new PaginaSolicitacoes(solicitacoes.listar(status,tamanho,(long)pagina*tamanho),pagina,tamanho,solicitacoes.contar(status));
    }

    SolicitacaoAdminResponse consultar(long id) {
        exigirAdmin();
        return buscar(id);
    }

    @Transactional
    public SolicitacaoAdminResponse decidir(long id, DecisaoRequest entrada) {
        long administrador = exigirAdmin();
        if (entrada.status() == StatusSolicitacao.PENDENTE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe APROVADA ou REJEITADA.");
        String motivo = entrada.motivo() == null ? null : entrada.motivo().strip();
        if (entrada.status() == StatusSolicitacao.REJEITADA && (motivo == null || motivo.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe o motivo da rejeição.");
        if (entrada.status() == StatusSolicitacao.APROVADA && motivo != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Motivo deve ser informado somente na rejeição.");
        if (!solicitacoes.decidir(id,entrada.status().name(),motivo,administrador)) {
            buscar(id);
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Solicitação já analisada.");
        }
        return buscar(id);
    }

    private SolicitacaoAdminResponse buscar(long id) {
        return solicitacoes.buscar(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitação de educadora não encontrada."));
    }

    private long exigirAdmin() {
        var identidade = acesso.identidade();
        if (!identidade.possui(Papel.ADMIN))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso restrito a administradores.");
        return identidade.id();
    }
}
