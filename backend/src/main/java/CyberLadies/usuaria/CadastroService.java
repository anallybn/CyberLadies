package CyberLadies.usuaria;

import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class CadastroService {
    private final UsuariaRepository usuarias;
    private final PasswordEncoder senhas;

    CadastroService(UsuariaRepository usuarias, PasswordEncoder senhas) {
        this.usuarias = usuarias;
        this.senhas = senhas;
    }

    void cadastrar(CadastroRequest cadastro) {
        try {
            usuarias.inserir(cadastro.nome().strip(), cadastro.email().strip().toLowerCase(Locale.ROOT),
                senhas.encode(cadastro.senha()),
                cadastro.competencias() == null ? "" : cadastro.competencias().strip());
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
    }
}
