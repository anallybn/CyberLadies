package CyberLadies.certificado.controller;

import CyberLadies.certificado.model.Certificado;

import CyberLadies.certificado.service.CertificadoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController

@RequestMapping("/api/certificados")
public class CertificadoController {

    // Serviço que contém as regras de certificados.
    private final CertificadoService certificadoService;

    // O Spring fornece automaticamente o serviço.
    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    // Recebe os dados de um certificado para validação.
    @PostMapping("/validar")
    public ResponseEntity<String> validarCertificado(
            @RequestBody Certificado certificado) {

        // Verifica o formato do código de validação.
        if (!certificadoService.validar(certificado)) {

            // Retorna HTTP 400 se o código for inválido.
            return ResponseEntity.badRequest()
                    .body("Código do certificado inválido.");
        }

        // Retorna HTTP 200 se o código tiver formato válido.
        return ResponseEntity.ok(
                "Formato do código do certificado válido."
        );
    }
}
