package CyberLadies.academy.service;
import CyberLadies.academy.dto.AlunasDados.*;
import CyberLadies.academy.repository.AlunasRepository;
import CyberLadies.auth.service.AcessoConta;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AlunasService {
    private final AcessoConta acesso; private final AlunasRepository repository;
    public AlunasService(AcessoConta acesso,AlunasRepository repository) {this.acesso=acesso;this.repository=repository;}
    private long aprovada() {
        var conta=acesso.educadora();
        if(!"APROVADA".equals(conta.statusEducadora()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"O acompanhamento das alunas está disponível somente para Educadoras aprovadas.");
        return conta.id();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Painel painel(String busca,String status,Long trilha) {
        long dona=aprovada();
        if(busca.length()>160 || !Set.of("TODAS","ANDAMENTO","CONCLUIDAS").contains(status) || trilha!=null&&trilha<=0)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Confira os filtros.");
        if(trilha!=null&&!repository.propria(dona,trilha))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,"Trilha não encontrada.");
        return new Painel(repository.indicadores(dona),repository.trilhas(dona),repository.listar(dona,busca.strip(),status,trilha));
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Detalhe detalhe(long aluna,long trilha) {
        long dona=aprovada();
        if(aluna<=0||trilha<=0)throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Inscrição inválida.");
        var inscricao=repository.inscricao(dona,aluna,trilha).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,"Inscrição não encontrada."));
        return new Detalhe(inscricao,repository.aulas(dona,aluna,trilha));
    }
}
