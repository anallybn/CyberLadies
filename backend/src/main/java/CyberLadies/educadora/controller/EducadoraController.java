package CyberLadies.educadora.controller;

import CyberLadies.educadora.model.Educadora;

import CyberLadies.educadora.service.EducadoraService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController

@RequestMapping("/api/educadoras")
public class EducadoraController {

    // Serviço responsável pelas regras de negócio.
    private final EducadoraService educadoraService;

    // O Spring fornece automaticamente o serviço.
    public EducadoraController(EducadoraService educadoraService) {
        this.educadoraService = educadoraService;
    }

    // Recebe os dados necessários para consultar
    // se uma educadora está aprovada para publicar.
    @PostMapping("/verificar-publicacao")
    public ResponseEntity<String> verificarPublicacao(
            @RequestBody VerificacaoPublicacaoRequest dados) {

        // Verifica se o status informado é válido.
        if (dados == null || dados.status() == null) {
            return ResponseEntity.badRequest()
                    .body("Status da educadora é obrigatório.");
        }

        // Este endpoint ainda não consulta o banco.
        // A autorização real deverá consultar a educadora
        // autenticada e seu status salvo no PostgreSQL.
        return ResponseEntity.ok(
                "Status recebido para validação."
        );
    }

    // Define os dados recebidos pela requisição.
    // Não recebe senha nem outros dados sensíveis.
    public record VerificacaoPublicacaoRequest(
            String status
    ) {
    }
}
