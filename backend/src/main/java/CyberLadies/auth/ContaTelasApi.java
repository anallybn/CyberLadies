package CyberLadies.auth;

import CyberLadies.auth.dto.ContaDados.Conta;
import CyberLadies.auth.service.AcessoConta;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Projeção de compatibilidade; não substitui /auth/me ou seus papéis. */
@RestController
class ContaTelasApi {
    private final AcessoConta acesso;
    ContaTelasApi(AcessoConta acesso) { this.acesso=acesso; }
    @GetMapping("/api/auth/conta") Conta conta() { return acesso.conta(); }
}
