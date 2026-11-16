package CyberLadies.vaga.controller;

// Importa o modelo que representa uma vaga de emprego.
import CyberLadies.vaga.model.Vaga;

// Importa o serviço responsável por validar as vagas.
import CyberLadies.vaga.service.VagaService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/vagas")
public class VagaController {

    // Serviço responsável pelas regras de negócio.
    private final VagaService vagaService;

    // O Spring Boot fornece automaticamente o serviço.
    public VagaController(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    // Recebe uma requisição POST para cadastrar uma nova vaga.
    @PostMapping
    public ResponseEntity<Vaga> cadastrarVaga(
            @RequestBody Vaga vaga) {

        // Verifica se os dados enviados são válidos.
        if (!vagaService.dadosValidos(vaga)) {

            // Retorna HTTP 400 se os dados forem inválidos.
            return ResponseEntity.badRequest().build();
        }

        // Cadastra a vaga utilizando o Service.
        Vaga novaVaga = vagaService.cadastrar(vaga);

        // Retorna HTTP 201 com os dados da vaga cadastrada.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novaVaga);
    }

    // Recebe uma requisição GET para listar todas as vagas.
    @GetMapping
    public ResponseEntity<List<Vaga>> listarVagas() {

        // Busca todas as vagas cadastradas no Service.
        List<Vaga> vagas = vagaService.listarTodos();

        // Retorna HTTP 200 com a lista de vagas.
        return ResponseEntity.ok(vagas);
    }


    // Recebe uma requisição GET para buscar uma vaga pelo ID.
    @GetMapping("/{id}")
    public ResponseEntity<Vaga> buscarVagaPorId(
            @PathVariable Long id) {

        // Procura a vaga pelo ID informado.
        Vaga vaga = vagaService.buscarPorId(id);

        // Retorna HTTP 404 se a vaga não existir.
        if (vaga == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com a vaga encontrada.
        return ResponseEntity.ok(vaga);
    }

    // Recebe uma requisição PUT para atualizar uma vaga.
    @PutMapping("/{id}")
    public ResponseEntity<Vaga> atualizarVaga(
            @PathVariable Long id,
            @RequestBody Vaga dadosAtualizados) {

        // Verifica se a vaga existe.
        if (vagaService.buscarPorId(id) == null) {

            // Retorna HTTP 404 se a vaga não for encontrada.
            return ResponseEntity.notFound().build();
        }

        // Verifica se os novos dados são válidos.
        if (!vagaService.dadosValidos(dadosAtualizados)) {

            // Retorna HTTP 400 se os dados forem inválidos.
            return ResponseEntity.badRequest().build();
        }

        // Atualiza a vaga utilizando o Service.
        Vaga vagaAtualizada =
                vagaService.atualizar(id, dadosAtualizados);

        // Retorna HTTP 200 com a vaga atualizada.
        return ResponseEntity.ok(vagaAtualizada);
    }


    // Recebe uma requisição DELETE para excluir uma vaga.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirVaga(
            @PathVariable Long id) {

        // Solicita ao Service a exclusão da vaga.
        boolean excluida = vagaService.excluir(id);

        // Retorna HTTP 404 se a vaga não existir.
        if (!excluida) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 204 quando a exclusão é realizada.
        return ResponseEntity.noContent().build();
    }

    // Recebe uma requisição POST para validar uma vaga.
    @PostMapping("/validar")
    public ResponseEntity<String> validarVaga(
            @RequestBody Vaga vaga) {

        // Verifica os dados usando o VagaService.
        if (!vagaService.dadosValidos(vaga)) {

            // Retorna HTTP 400 se os dados forem inválidos.
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Dados da vaga inválidos.");
        }

        // Retorna HTTP 200 se os dados forem válidos.
        return ResponseEntity
                .ok("Dados da vaga válidos.");
    }
}
