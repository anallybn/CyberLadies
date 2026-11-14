package CyberLadies.empresa.controller;

import CyberLadies.empresa.model.Empresa;
import CyberLadies.empresa.service.EmpresaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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


    // Recebe uma requisição POST para cadastrar uma empresa.
    @PostMapping
    public ResponseEntity<Empresa> cadastrarEmpresa(
            @RequestBody Empresa empresa) {

        // Solicita ao Service o cadastro da empresa.
        Empresa novaEmpresa = empresaService.cadastrar(empresa);

        // Retorna HTTP 400 se os dados forem inválidos.
        if (novaEmpresa == null) {
            return ResponseEntity.badRequest().build();
        }

        // Retorna HTTP 201 com a empresa cadastrada.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novaEmpresa);
    }

    // Recebe uma requisição GET para listar todas as empresas.
    @GetMapping
    public ResponseEntity<List<Empresa>> listarEmpresas() {

        // Solicita ao Service a lista de empresas cadastradas.
        List<Empresa> empresas = empresaService.listarTodos();

        // Retorna HTTP 200 com a lista de empresas.
        return ResponseEntity.ok(empresas);
    }


    // Recebe uma requisição GET para buscar uma empresa pelo ID.
    @GetMapping("/{id}")
    public ResponseEntity<Empresa> buscarEmpresaPorId(
            @PathVariable Long id) {

        // Solicita ao Service a empresa com o ID informado.
        Empresa empresa = empresaService.buscarPorId(id);

        // Retorna HTTP 404 se a empresa não existir.
        if (empresa == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com os dados da empresa.
        return ResponseEntity.ok(empresa);
    }

    // Recebe uma requisição PUT para atualizar uma empresa.
    @PutMapping("/{id}")
    public ResponseEntity<Empresa> atualizarEmpresa(
            @PathVariable Long id,
            @RequestBody Empresa dadosAtualizados) {

        // Verifica se a empresa existe.
        Empresa empresaExistente = empresaService.buscarPorId(id);

        // Retorna HTTP 404 se a empresa não existir.
        if (empresaExistente == null) {
            return ResponseEntity.notFound().build();
        }

        // Solicita ao Service a atualização dos dados.
        Empresa empresaAtualizada =
                empresaService.atualizar(id, dadosAtualizados);

        // Retorna HTTP 400 se os novos dados forem inválidos.
        if (empresaAtualizada == null) {
            return ResponseEntity.badRequest().build();
        }

        // Retorna HTTP 200 com os dados atualizados.
        return ResponseEntity.ok(empresaAtualizada);
    }

    // Recebe uma requisição DELETE para excluir uma empresa.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirEmpresa(
            @PathVariable Long id) {

        // Solicita ao Service a exclusão da empresa.
        boolean excluida = empresaService.excluir(id);

        // Retorna HTTP 404 se a empresa não existir.
        if (!excluida) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 204 quando a exclusão é realizada.
        return ResponseEntity.noContent().build();
    }


}
