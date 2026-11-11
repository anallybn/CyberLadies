package CyberLadies.avaliacao.controller;

import CyberLadies.avaliacao.service.AvaliacaoService;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController

@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    // Serviço responsável pelas regras de avaliação.
    private final AvaliacaoService avaliacaoService;

    // O Spring fornece automaticamente o serviço.
    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    // Recebe uma nota para verificar se é válida.
    @PostMapping("/validar-nota")
    public ResponseEntity<String> validarNota(
            @RequestBody ValidarNotaRequest dados) {

        // Impede o envio de uma nota inexistente.
        if (dados == null) {
            return ResponseEntity.badRequest()
                    .body("Informe uma nota.");
        }

        // Verifica se a nota está entre 1 e 5.
        if (!avaliacaoService.notaValida(dados.nota())) {
            return ResponseEntity.badRequest()
                    .body("A nota deve estar entre 1 e 5.");
        }

        // Retorna sucesso quando a nota é válida.
        return ResponseEntity.ok("Nota válida.");
    }

    // Define os dados recebidos na requisição.
    public record ValidarNotaRequest(Integer nota) {
    }
}
