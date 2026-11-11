package CyberLadies.vaga.controller;

// Importa o modelo que representa uma vaga de emprego.
import CyberLadies.vaga.model.Vaga;

// Importa o serviço responsável por validar as vagas.
import CyberLadies.vaga.service.VagaService;

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
