package CyberLadies.educadora;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/educadora/solicitacao")
class EducadoraApi {
    private final SolicitacaoService solicitacoes;

    EducadoraApi(SolicitacaoService solicitacoes) { this.solicitacoes = solicitacoes; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SolicitacaoResponse solicitar(@Valid @RequestBody SolicitacaoRequest entrada) {
        return solicitacoes.solicitar(entrada);
    }

    @GetMapping
    SolicitacaoResponse consultar() { return solicitacoes.consultar(); }
}
