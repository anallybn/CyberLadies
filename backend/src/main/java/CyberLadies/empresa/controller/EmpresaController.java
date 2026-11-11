package CyberLadies.empresa.controller;

import CyberLadies.empresa.model.Empresa;

import CyberLadies.empresa.service.EmpresaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController


@RequestMapping("/api/empresas")
public class EmpresaController {

    // Serviço responsável pelas regras de negócio.
    private final EmpresaService empresaService;

    // O Spring fornece automaticamente o serviço.
    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    // Recebe os dados de uma empresa para validação.
    @PostMapping("/validar")
    public ResponseEntity<String> validarEmpresa(
            @RequestBody Empresa empresa) {

        // Verifica os dados usando o EmpresaService.
        if (!empresaService.dadosValidos(empresa)) {

            // Retorna HTTP 400 se os dados forem inválidos.
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Dados da empresa inválidos.");
        }

        // Retorna HTTP 200 se os dados forem válidos.
        return ResponseEntity
                .ok("Dados da empresa válidos.");
    }
}
