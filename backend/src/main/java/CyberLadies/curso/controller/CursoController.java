package CyberLadies.curso.controller;

// Importa o modelo que representa um curso.
import CyberLadies.curso.model.Curso;

// Importa o serviço responsável por validar cursos.
import CyberLadies.curso.service.CursoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController

// Define o endereço principal da API de cursos.
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoService cursoService;

    // O Spring fornece automaticamente o serviço.
    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    // Recebe uma requisição POST para validar um curso.
    @PostMapping("/validar")
    public ResponseEntity<String> validarCurso(
            @RequestBody Curso curso) {

        // Verifica os dados usando o CursoService.
        if (!cursoService.dadosValidos(curso)) {

            // Retorna erro HTTP 400 se os dados forem inválidos.
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Dados do curso inválidos.");
        }

        // Retorna HTTP 200 se os dados forem válidos.
        return ResponseEntity
                .ok("Dados do curso válidos.");
    }
}