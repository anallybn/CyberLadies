package CyberLadies.auth.service;

import CyberLadies.auth.dto.ContaDados.*;
import CyberLadies.auth.repository.ContaRepository;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContaService {
    private final ContaRepository repository;
    private final Pbkdf2PasswordEncoder senhas=Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    public ContaService(ContaRepository repository) { this.repository=repository; }
    @Transactional public void cadastrar(CadastroUsuaria dados) {
        try { criar(dados.nome(),dados.email(),dados.senha(),Tipo.CYBERLADIE,dados.competencias()==null?"":dados.competencias().strip()); }
        catch(DuplicateKeyException e) { throw duplicado(); }
    }
    @Transactional public void cadastrar(CadastroEducadora dados) {
        try {
            long id=criar(dados.nome(),dados.email(),dados.senha(),Tipo.EDUCADORA,"");
            repository.educadora(id,dados);
        } catch(DuplicateKeyException e) { throw duplicado(); }
    }
    @Transactional public void cadastrar(CadastroEmpresa dados) {
        if(!cnpjValido(dados.cnpj())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe um CNPJ válido.");
        try {
            long id=criar(dados.nome(),dados.email(),dados.senha(),Tipo.EMPRESA,"");
            repository.empresa(id,dados);
        } catch(DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"E-mail ou CNPJ já cadastrado.");
        }
    }
    @Transactional public Sessao entrar(Login dados) {
        var credencial=repository.credencial(normalizar(dados.email()));
        if(credencial.isEmpty() || !credencial.get().cadastroCompleto() || !senhas.matches(dados.senha(),credencial.get().hash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"E-mail ou senha incorretos.");
        var conta=credencial.get();
        String token=Credenciais.token();
        repository.criarSessao(conta.id(),Credenciais.hash(token));
        return new Sessao(token,conta.tipo(),conta.statusEducadora());
    }
    public void sair(String header) { repository.encerrarSessao(Credenciais.hashBearer(header)); }
    private long criar(String nome,String email,String senha,Tipo tipo,String competencias) {
        return repository.cadastrar(nome.strip(),normalizar(email),senhas.encode(senha),tipo,competencias);
    }
    private String normalizar(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private ResponseStatusException duplicado() { return new ResponseStatusException(HttpStatus.CONFLICT,"E-mail já cadastrado."); }
    private boolean cnpjValido(String cnpj) {
        if(cnpj==null || !cnpj.matches("[0-9]{14}") || cnpj.matches("(\\d)\\1+")) return false;
        String base=cnpj.substring(0,12);
        int primeiro=digito(base);
        return primeiro==cnpj.charAt(12)-'0' && digito(base+primeiro)==cnpj.charAt(13)-'0';
    }
    private int digito(String base) {
        int soma=0,peso=base.length()-7;
        for(char numero:base.toCharArray()) { soma+=(numero-'0')*peso--; if(peso<2) peso=9; }
        int resto=soma%11;
        return resto<2?0:11-resto;
    }
}