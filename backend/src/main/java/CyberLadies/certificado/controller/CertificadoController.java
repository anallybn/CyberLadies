package CyberLadies.certificado.controller;

import CyberLadies.certificado.model.Certificado;
import CyberLadies.certificado.service.CertificadoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus; // Permite retornar HTTP 201.

import java.util.List; // Permite retornar uma lista de certificados.

@RestController

@RequestMapping("/api/certificados")
public class CertificadoController {

    // Serviço que contém as regras de certificados.
    private final CertificadoService certificadoService;

    // O Spring fornece automaticamente o serviço.
    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    // Recebe os dados para cadastrar um novo certificado.
    @PostMapping
    public ResponseEntity<Certificado> cadastrarCertificado(
            @RequestBody Certificado dados) {

        // Tenta cadastrar o certificado usando o Service.
        try {
            Certificado novoCertificado = certificadoService.cadastrar(
                    dados.getUsuariaId(),
                    dados.getNomeAtividade()
            );

            // Retorna HTTP 201 com o certificado criado.
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(novoCertificado);

        } catch (IllegalArgumentException erro) {

            // Retorna HTTP 400 quando os dados são inválidos.
            return ResponseEntity.badRequest().build();
        }
    }

    // Retorna todos os certificados cadastrados.
    @GetMapping
    public ResponseEntity<List<Certificado>> listarCertificados() {

        // Solicita ao Service a lista de certificados.
        List<Certificado> certificados =
                certificadoService.listarTodos();

        // Retorna HTTP 200 com a lista encontrada.
        return ResponseEntity.ok(certificados);
    }


    // Busca um certificado pelo seu identificador.
    @GetMapping("/{id}")
    public ResponseEntity<Certificado> buscarCertificadoPorId(
            @PathVariable Long id) {

        // Solicita ao Service o certificado correspondente ao ID.
        Certificado certificado = certificadoService.buscarPorId(id);

        // Retorna HTTP 404 caso o certificado não exista.
        if (certificado == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com os dados do certificado encontrado.
        return ResponseEntity.ok(certificado);
    }


    // Atualiza os dados de um certificado existente.
    @PutMapping("/{id}")
    public ResponseEntity<Certificado> atualizarCertificado(
            @PathVariable Long id,
            @RequestBody Certificado dadosAtualizados) {

        // Verifica se o certificado existe.
        Certificado certificadoExistente =
                certificadoService.buscarPorId(id);

        // Retorna HTTP 404 se não encontrar o certificado.
        if (certificadoExistente == null) {
            return ResponseEntity.notFound().build();
        }

        // Solicita ao Service a atualização dos dados.
        Certificado certificadoAtualizado =
                certificadoService.atualizar(id, dadosAtualizados);

        // Retorna HTTP 400 se os novos dados forem inválidos.
        if (certificadoAtualizado == null) {
            return ResponseEntity.badRequest().build();
        }

        // Retorna HTTP 200 com o certificado atualizado.
        return ResponseEntity.ok(certificadoAtualizado);
    }


    // Exclui um certificado pelo seu identificador.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCertificado(
            @PathVariable Long id) {

        // Solicita ao Service a exclusão do certificado.
        boolean excluido = certificadoService.excluir(id);

        // Retorna HTTP 404 se o certificado não existir.
        if (!excluido) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 204 quando a exclusão é realizada.
        return ResponseEntity.noContent().build();
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
