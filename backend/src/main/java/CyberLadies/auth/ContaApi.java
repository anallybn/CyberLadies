package CyberLadies.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
class ContaApi {
    private final SessaoService sessaoService;
    private final AcessoAutenticado acesso;

    ContaApi(SessaoService sessaoService, AcessoAutenticado acesso) {
        this.sessaoService = sessaoService;
        this.acesso = acesso;
    }

    @GetMapping("/auth/me")
    AcessoAutenticado.Identidade me() { return acesso.identidade(); }

    @PostMapping("/auth/login")
    Map<String, String> entrar(@RequestBody @Valid LoginRequest login) {
        return Map.of("token", sessaoService.entrar(login));
    }

    @DeleteMapping("/auth/sessao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sair(@RequestHeader(value="Authorization", defaultValue="") String auth) {
        sessaoService.sair(auth);
    }
}
