package CyberLadies.educadora.controller;

import CyberLadies.educadora.service.EducadoraService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import CyberLadies.educadora.model.Educadora;
import org.springframework.http.HttpStatus;


import java.util.List;


@RestController

@RequestMapping("/api/educadoras")
public class EducadoraController {

    // Serviço responsável pelas regras de negócio.
    private final EducadoraService educadoraService;

    // O Spring fornece automaticamente o serviço.
    public EducadoraController(EducadoraService educadoraService) {
        this.educadoraService = educadoraService;
    }

    // Recebe os dados e cadastra uma nova educadora.
    @PostMapping
    public ResponseEntity<Educadora> cadastrarEducadora(
            @RequestBody Educadora educadora) {

        // Solicita ao serviço o cadastro da educadora.
        Educadora novaEducadora = educadoraService.cadastrar(educadora);

        // Retorna 400 quando os dados são inválidos.
        if (novaEducadora == null) {
            return ResponseEntity.badRequest().build();
        }

        // Retorna 201 quando o cadastro é realizado.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(novaEducadora);
    }

    // Retorna todas as educadoras cadastradas.
    @GetMapping
    public ResponseEntity<List<Educadora>> listarEducadoras() {

        // Solicita ao serviço a lista de educadoras.
        List<Educadora> educadoras = educadoraService.listarTodas();

        // Retorna HTTP 200 com as educadoras encontradas.
        return ResponseEntity.ok(educadoras);
    }


    // Busca uma educadora pelo seu identificador.
    @GetMapping("/{id}")
    public ResponseEntity<Educadora> buscarEducadoraPorId(
            @PathVariable Long id) {

        // Solicita ao serviço a educadora correspondente ao ID.
        Educadora educadora = educadoraService.buscarPorId(id);

        // Retorna HTTP 404 se a educadora não existir.
        if (educadora == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com os dados da educadora.
        return ResponseEntity.ok(educadora);
    }


    // Atualiza os dados de uma educadora cadastrada.
    @PutMapping("/{id}")
    public ResponseEntity<Educadora> atualizarEducadora(
            @PathVariable Long id,
            @RequestBody Educadora dadosAtualizados) {

        // Verifica se a educadora existe.
        Educadora educadoraExistente = educadoraService.buscarPorId(id);

        // Retorna HTTP 404 se não encontrar a educadora.
        if (educadoraExistente == null) {
            return ResponseEntity.notFound().build();
        }

        // Solicita ao serviço a atualização dos dados.
        Educadora educadoraAtualizada =
                educadoraService.atualizar(id, dadosAtualizados);

        // Retorna HTTP 400 se os dados forem inválidos.
        if (educadoraAtualizada == null) {
            return ResponseEntity.badRequest().build();
        }

        // Retorna HTTP 200 com os dados atualizados.
        return ResponseEntity.ok(educadoraAtualizada);
    }

    // Exclui uma educadora pelo seu identificador.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirEducadora(
            @PathVariable Long id) {

        // Solicita ao serviço a exclusão da educadora.
        boolean excluida = educadoraService.excluir(id);

        // Retorna HTTP 404 se a educadora não existir.
        if (!excluida) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 204 quando a exclusão é realizada.
        return ResponseEntity.noContent().build();
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
