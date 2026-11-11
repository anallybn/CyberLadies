package CyberLadies.candidatura.controller;

import CyberLadies.candidatura.model.Candidatura;

import CyberLadies.candidatura.service.CandidaturaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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
}
