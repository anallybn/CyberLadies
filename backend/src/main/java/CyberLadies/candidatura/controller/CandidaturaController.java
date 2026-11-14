package CyberLadies.candidatura.controller;

import CyberLadies.candidatura.model.Candidatura;

import CyberLadies.candidatura.service.CandidaturaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;



@RestController

@RequestMapping("/api/candidaturas")
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    // O Spring Boot fornece automaticamente o serviço.
    public CandidaturaController(CandidaturaService candidaturaService) {
        this.candidaturaService = candidaturaService;
    }

    // Recebe uma requisição POST para validar uma candidatura.
    @PostMapping("/validar")
    public ResponseEntity<String> validarCandidatura(
            @RequestBody Candidatura candidatura) {

        // Verifica os dados usando o CandidaturaService.
        if (!candidaturaService.dadosValidos(candidatura)) {

            // Retorna HTTP 400 se os dados forem inválidos.
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Dados da candidatura inválidos.");
        }

        // Retorna HTTP 200 se os dados forem válidos.
        return ResponseEntity
                .ok("Dados da candidatura válidos.");
    }

    // Recebe uma requisição POST para cadastrar uma candidatura.
    @PostMapping
    public ResponseEntity<Candidatura> cadastrarCandidatura(
            @RequestBody Candidatura candidatura) {

        // Solicita ao Service o cadastro da candidatura.
        Candidatura novaCandidatura =
                candidaturaService.cadastrar(candidatura);

        // Retorna HTTP 400 se os dados forem inválidos.
        if (novaCandidatura == null) {
            return ResponseEntity.badRequest().build();
        }

        // Retorna HTTP 201 quando o cadastro é realizado.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novaCandidatura);
    }


    // Recebe uma requisição GET para listar todas as candidaturas.
    @GetMapping
    public ResponseEntity<List<Candidatura>> listarCandidaturas() {

        // Solicita ao Service todas as candidaturas cadastradas.
        List<Candidatura> candidaturas =
                candidaturaService.listarTodos();

        // Retorna HTTP 200 com a lista de candidaturas.
        return ResponseEntity.ok(candidaturas);
    }


    // Recebe uma requisição GET para buscar uma candidatura pelo ID.
    @GetMapping("/{id}")
    public ResponseEntity<Candidatura> buscarCandidaturaPorId(
            @PathVariable Long id) {

        // Solicita ao Service a candidatura com o ID informado.
        Candidatura candidatura =
                candidaturaService.buscarPorId(id);

        // Retorna HTTP 404 se a candidatura não existir.
        if (candidatura == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com os dados da candidatura.
        return ResponseEntity.ok(candidatura);
    }

    // Recebe uma requisição PUT para atualizar uma candidatura.
    @PutMapping("/{id}")
    public ResponseEntity<Candidatura> atualizarCandidatura(
            @PathVariable Long id,
            @RequestBody Candidatura dadosAtualizados) {

        // Verifica se a candidatura existe.
        Candidatura candidaturaExistente =
                candidaturaService.buscarPorId(id);

        // Retorna HTTP 404 se a candidatura não existir.
        if (candidaturaExistente == null) {
            return ResponseEntity.notFound().build();
        }

        // Verifica se os novos dados são válidos.
        if (!candidaturaService.dadosValidos(dadosAtualizados)) {
            return ResponseEntity.badRequest().build();
        }

        // Solicita ao Service a atualização da candidatura.
        Candidatura candidaturaAtualizada =
                candidaturaService.atualizar(id, dadosAtualizados);

        // Retorna HTTP 200 com os dados atualizados.
        return ResponseEntity.ok(candidaturaAtualizada);
    }


    // Recebe uma requisição DELETE para excluir uma candidatura.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCandidatura(
            @PathVariable Long id) {

        // Solicita ao Service a exclusão da candidatura.
        boolean excluida = candidaturaService.excluir(id);

        // Retorna HTTP 404 se a candidatura não existir.
        if (!excluida) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 204 quando a exclusão é realizada.
        return ResponseEntity.noContent().build();
    }
}
