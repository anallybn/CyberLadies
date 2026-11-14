package CyberLadies.avaliacao.controller;

import CyberLadies.avaliacao.service.AvaliacaoService;

import CyberLadies.avaliacao.model.Avaliacao; // Representa os dados de uma avaliação.
import java.util.List; // Permite retornar uma lista de avaliações.

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

    // Recebe uma requisição para cadastrar uma avaliação.
    @PostMapping
    public ResponseEntity<Avaliacao> cadastrar(
            @RequestBody Avaliacao dados) {

        // Envia os dados ao Service para validar e cadastrar.
        Avaliacao novaAvaliacao = avaliacaoService.cadastrar(dados);

        // Retorna HTTP 201 quando o cadastro é realizado.
        return ResponseEntity.status(201).body(novaAvaliacao);
    }

    // Retorna todas as avaliações cadastradas.
    @GetMapping
    public ResponseEntity<List<Avaliacao>> listarTodos() {

        // Busca a lista de avaliações no Service.
        List<Avaliacao> avaliacoes = avaliacaoService.listarTodos();

        // Retorna HTTP 200 com a lista de avaliações.
        return ResponseEntity.ok(avaliacoes);
    }

    // Busca uma avaliação pelo seu identificador.
    @GetMapping("/{id}")
    public ResponseEntity<Avaliacao> buscarPorId(
            @PathVariable Long id) {

        // Solicita ao Service a avaliação correspondente.
        Avaliacao avaliacao = avaliacaoService.buscarPorId(id);

        // Retorna HTTP 404 se a avaliação não existir.
        if (avaliacao == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com a avaliação encontrada.
        return ResponseEntity.ok(avaliacao);
    }

    // Atualiza uma avaliação existente pelo ID.
    @PutMapping("/{id}")
    public ResponseEntity<Avaliacao> atualizar(
            @PathVariable Long id,
            @RequestBody Avaliacao dados) {

        // Envia os novos dados ao Service.
        Avaliacao atualizada = avaliacaoService.atualizar(id, dados);

        // Retorna HTTP 404 se a avaliação não existir.
        if (atualizada == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com a avaliação atualizada.
        return ResponseEntity.ok(atualizada);
    }


    // Exclui uma avaliação pelo seu identificador.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id) {

        // Solicita ao Service a exclusão da avaliação.
        boolean excluida = avaliacaoService.excluir(id);

        // Retorna HTTP 404 se a avaliação não existir.
        if (!excluida) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 204 quando a exclusão é realizada.
        return ResponseEntity.noContent().build();
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
