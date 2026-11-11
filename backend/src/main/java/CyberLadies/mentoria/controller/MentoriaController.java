package CyberLadies.mentoria.controller;

import CyberLadies.mentoria.model.Mentoria;

import CyberLadies.mentoria.service.MentoriaService;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController

@RequestMapping("/api/mentorias")
public class MentoriaController {

    // Serviço que contém as regras de negócio.
    private final MentoriaService mentoriaService;

    // O Spring injeta automaticamente o serviço.
    public MentoriaController(MentoriaService mentoriaService) {
        this.mentoriaService = mentoriaService;
    }

    // Recebe uma requisição POST para validar uma mentoria.
    @PostMapping("/validar")
    public ResponseEntity<String> validarMentoria(
            @RequestBody Mentoria mentoria) {

        // Verifica os dados usando o MentoriaService.
        if (!mentoriaService.dadosValidos(mentoria)) {

            // Retorna HTTP 400 quando os dados são inválidos.
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Dados da mentoria inválidos.");
        }

        // Retorna HTTP 200 quando os dados são válidos.
        return ResponseEntity
                .ok("Dados da mentoria válidos.");
    }
}

