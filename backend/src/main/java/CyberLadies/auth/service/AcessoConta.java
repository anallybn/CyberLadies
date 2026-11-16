package CyberLadies.auth.service;

import CyberLadies.auth.dto.ContaDados.*;
import CyberLadies.auth.repository.ContaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcessoConta {
    private final ContaRepository repository;
    private final HttpServletRequest request;
    public AcessoConta(ContaRepository repository,HttpServletRequest request) { this.repository=repository; this.request=request; }
    public Conta conta() {
        return repository.conta(Credenciais.hashBearer(request.getHeader("Authorization")))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sessão expirada."));
    }
    public Conta cyberLadie() {
        var conta=conta();
        if(conta.perfil()!=Tipo.CYBERLADIE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta funcionalidade é exclusiva para CyberLadie.");
        return conta;
    }
    public Conta educadora() {
        var conta=conta();
        if(conta.perfil()!=Tipo.EDUCADORA || conta.statusEducadora()==null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso exclusivo da Educadora.");
        return conta;
    }
    public Conta estudante() {
        var conta=conta();
        if(conta.perfil()!=Tipo.CYBERLADIE && conta.perfil()!=Tipo.EDUCADORA)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta conta não possui acesso de estudante ou participante.");
        return conta;
    }
}