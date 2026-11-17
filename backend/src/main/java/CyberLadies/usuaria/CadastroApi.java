package CyberLadies.usuaria;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
class CadastroApi {
    private final CadastroService cadastroService;

    CadastroApi(CadastroService cadastroService) { this.cadastroService = cadastroService; }

    @PostMapping("/api/auth/cadastro/usuaria")
    @ResponseStatus(HttpStatus.CREATED)
    void cadastrar(@RequestBody @Valid CadastroRequest cadastro) {
        cadastroService.cadastrar(cadastro);
    }
}
