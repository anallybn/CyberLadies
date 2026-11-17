package CyberLadies.auth;

import CyberLadies.auth.dto.ContaDados.CadastroEmpresa;


import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Cadastro da empresa da origem, integrado à conta e aos papéis existentes. */
@Service
class EmpresaCadastroService {
    private final EmpresaCadastroRepository repository;
    private final PasswordEncoder senhas;
    EmpresaCadastroService(EmpresaCadastroRepository repository, PasswordEncoder senhas) { this.repository=repository; this.senhas=senhas; }
    @Transactional
    public void cadastrar(CadastroEmpresa dados) {
        if(!cnpjValido(dados.cnpj())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe um CNPJ válido.");
        try {
            repository.inserir(dados,senhas.encode(dados.senha()));
        } catch(DuplicateKeyException erro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"E-mail ou CNPJ já cadastrado.");
        }
    }
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
