package CyberLadies.educadora;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/educadoras/solicitacoes")
class AnaliseApi {
    private final AnaliseService analise;

    AnaliseApi(AnaliseService analise) { this.analise = analise; }

    @GetMapping
    PaginaSolicitacoes listar(@RequestParam(required=false) StatusSolicitacao status,
                             @RequestParam(defaultValue="0") int pagina,
                             @RequestParam(defaultValue="20") int tamanho) {
        return analise.listar(status,pagina,tamanho);
    }

    @GetMapping("/{contaId}")
    SolicitacaoAdminResponse consultar(@PathVariable long contaId) { return analise.consultar(contaId); }

    @PostMapping("/{contaId}/decisao")
    SolicitacaoAdminResponse decidir(@PathVariable long contaId, @Valid @RequestBody DecisaoRequest decisao) {
        return analise.decidir(contaId,decisao);
    }
}
