package CyberLadies.academy.service;

import CyberLadies.academy.dto.AcademyDados.Certificado;
import CyberLadies.academy.repository.AcademyRepository;
import CyberLadies.auth.service.AcessoConta;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CertificadoAcademyService {
    private final AcademyRepository repository;
    private final AcessoConta acesso;
    public CertificadoAcademyService(AcademyRepository repository,AcessoConta acesso) { this.repository=repository; this.acesso=acesso; }
    public List<Certificado> meus() { return repository.certificados(acesso.estudante().id()); }
    public Certificado buscar(UUID id) {
        return meus().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND,"Certificado não encontrado."));
    }
}