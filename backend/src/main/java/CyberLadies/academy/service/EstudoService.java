package CyberLadies.academy.service;

import CyberLadies.academy.dto.AcademyDados.*;
import CyberLadies.academy.repository.AcademyRepository;
import CyberLadies.auth.service.AcessoConta;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstudoService {
    private final AcademyRepository repository;
    private final AcessoConta acesso;
    private final AcademyService conteudo;
    public EstudoService(AcademyRepository repository,AcessoConta acesso,AcademyService conteudo) { this.repository=repository; this.acesso=acesso; this.conteudo=conteudo; }
    @Transactional
    public Progresso inscrever(long trilha) {
        long usuaria=acesso.estudante().id();
        conteudo.trilha(trilha);
        repository.bloquearTrilha(trilha);
        var dados=conteudo.trilha(trilha);
        if (dados.totalAulas()==0) throw erro(HttpStatus.CONFLICT,"Esta trilha ainda não possui aulas.");
        if (!repository.inscrever(usuaria,trilha)) throw erro(HttpStatus.CONFLICT,"Você já está inscrita nesta trilha.");
        return calcular(usuaria,trilha);
    }
    public List<Inscricao> inscricoes() { return repository.inscricoes(acesso.estudante().id()); }
    public List<Progresso> meusProgressos() {
        long usuaria=acesso.estudante().id();
        return repository.inscricoes(usuaria).stream().map(i->calcular(usuaria,i.trilhaId())).toList();
    }
    public Progresso progresso(long trilha) {
        long usuaria=acesso.estudante().id();
        conteudo.trilha(trilha);
        exigirInscricao(usuaria,trilha);
        return calcular(usuaria,trilha);
    }
    @Transactional
    public Progresso concluir(long id) {
        long usuaria=acesso.estudante().id();
        var aula=conteudo.aula(id).aula();
        exigirInscricao(usuaria,aula.trilhaId());
        repository.bloquearInscricao(usuaria,aula.trilhaId());
        repository.concluir(usuaria,aula);
        return calcular(usuaria,aula.trilhaId());
    }
    private org.springframework.web.server.ResponseStatusException erro(HttpStatus status,String mensagem) { return new org.springframework.web.server.ResponseStatusException(status,mensagem); }
    private void exigirInscricao(long usuaria,long trilha) {
        if (!repository.inscrita(usuaria,trilha)) throw erro(HttpStatus.FORBIDDEN,"Inscreva-se na trilha para registrar seu progresso.");
    }
    private Progresso calcular(long usuaria,long trilha) {
        var aulas=repository.aulas(trilha);
        var feitas=repository.concluidas(usuaria,trilha);
        int percentual=aulas.isEmpty()?0:(int)(100L*feitas.size()/aulas.size());
        Long proxima=aulas.stream().filter(a->!feitas.contains(a.id())).map(Aula::id).findFirst().orElse(null);
        return new Progresso(trilha,aulas.size(),feitas,percentual,proxima);
    }
}