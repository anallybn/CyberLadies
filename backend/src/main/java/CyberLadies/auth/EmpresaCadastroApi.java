package CyberLadies.auth;

import CyberLadies.auth.dto.ContaDados.CadastroEmpresa;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Cadastro da empresa da origem, integrado à conta e aos papéis existentes. */
@RestController
class EmpresaCadastroApi {
    private final JdbcClient db;
    private final PasswordEncoder senhas;
    EmpresaCadastroApi(JdbcClient db, PasswordEncoder senhas) { this.db=db; this.senhas=senhas; }
    @PostMapping("/api/auth/cadastro/empresa")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public void cadastrar(@Valid @RequestBody CadastroEmpresa dados) {
        if(!cnpjValido(dados.cnpj())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe um CNPJ válido.");
        try {
            long id=db.sql("INSERT INTO conta(nome,email,senha_hash) VALUES (?,?,?) RETURNING id")
                .params(dados.nome().strip(),dados.email().strip().toLowerCase(Locale.ROOT),senhas.encode(dados.senha())).query(Long.class).single();
            db.sql("INSERT INTO conta_papel VALUES (?,'EMPRESA')").param(id).update();
            long empresa=db.sql("INSERT INTO cyber_empresa(nome) VALUES (?) RETURNING id").param(dados.nome().strip()).query(Long.class).single();
            db.sql("INSERT INTO conta_empresa VALUES (?,?,?,?)").params(id,empresa,dados.cnpj(),dados.areaAtuacao().strip()).update();
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
