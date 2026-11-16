package CyberLadies.academy.service;

import CyberLadies.academy.dto.DashboardEducadoraDados.Dashboard;
import CyberLadies.academy.repository.DashboardEducadoraRepository;
import CyberLadies.auth.service.AcessoConta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardEducadoraService {
    private final AcessoConta acesso;
    private final DashboardEducadoraRepository repository;
    public DashboardEducadoraService(AcessoConta acesso,DashboardEducadoraRepository repository) {
        this.acesso=acesso; this.repository=repository;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Dashboard dashboard() {
        var conta=acesso.educadora();
        return new Dashboard(conta.nome(),conta.statusEducadora(),false,repository.indicadores(conta.id()),
            repository.trilhas(conta.id()),repository.mentorias(conta.id()),repository.materiais(conta.id()));
    }
}
