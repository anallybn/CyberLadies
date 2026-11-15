package CyberLadies.educadora;

import CyberLadies.auth.AcessoAutenticado;
import CyberLadies.auth.Papel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class SolicitacaoService {
    private final AcessoAutenticado acesso;
    private final EducadoraRepository educadoras;

    SolicitacaoService(AcessoAutenticado acesso, EducadoraRepository educadoras) {
        this.acesso = acesso;
        this.educadoras = educadoras;
    }

    @Transactional
    public SolicitacaoResponse solicitar(SolicitacaoRequest entrada) {
        long id = acesso.identidade().id();
        if (!educadoras.bloquearConta(id))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada.");
        // Reconsulta os papéis após adquirir o lock, evitando decisão baseada em leitura anterior.
        exigirUsuaria(acesso.identidade());
        if (educadoras.buscar(id).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitação de educadora já cadastrada.");
        var resposta = educadoras.inserir(id, entrada.especialidade().strip(), entrada.instituicao().strip());
        educadoras.atribuirPapel(id);
        return resposta;
    }

    SolicitacaoResponse consultar() {
        var identidade = acesso.identidade();
        exigirUsuaria(identidade);
        return educadoras.buscar(identidade.id()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação de educadora não encontrada."));
    }

    private void exigirUsuaria(AcessoAutenticado.Identidade identidade) {
        if (!identidade.possui(Papel.USUARIA) || identidade.possui(Papel.EMPRESA))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A solicitação exige uma conta de usuária.");
    }
}
