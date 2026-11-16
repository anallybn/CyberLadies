package CyberLadies.candidatura.service;
import CyberLadies.candidatura.model.Candidatura;
import CyberLadies.candidatura.repository.CandidaturaRepository;
import CyberLadies.auth.service.AcessoConta;
import CyberLadies.vaga.service.VagaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class CandidaturaService {
    private final CandidaturaRepository repository;
    private final AcessoConta acesso;
    private final VagaService vagas;
    public CandidaturaService(CandidaturaRepository repository,AcessoConta acesso,VagaService vagas) {
        this.repository=repository; this.acesso=acesso; this.vagas=vagas;
    }
    public List<Candidatura> listar() { return repository.listar(acesso.cyberLadie().id()); }
    public Candidatura buscar(long id) {
        long usuaria=acesso.cyberLadie().id();
        return repository.buscar(id,usuaria).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND,"Candidatura não encontrada."));
    }
    @Transactional
    public Candidatura cadastrar(long vagaId) {
        long usuaria=acesso.cyberLadie().id();
        vagas.buscarPorId(vagaId);
        var criada=repository.cadastrar(usuaria,vagaId);
        if (criada.isEmpty()) {
            vagas.buscarPorId(vagaId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Você já se candidatou a esta vaga.");
        }
        return criada.get();
    }
    public void bloquearAlteracao() {
        acesso.cyberLadie().id();
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Alterações de candidaturas não estão disponíveis nesta etapa.");
    }
}