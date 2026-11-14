package CyberLadies.mentoria.controller;

import CyberLadies.mentoria.model.Mentoria;

import CyberLadies.mentoria.service.MentoriaService;

import java.util.List;

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

    
    // Recebe uma requisição POST para cadastrar uma mentoria.
    @PostMapping
    public ResponseEntity<Mentoria> cadastrarMentoria(
            @RequestBody Mentoria mentoria) {

        // Verifica se os dados enviados são válidos.
        if (!mentoriaService.dadosValidos(mentoria)) {

            // Retorna HTTP 400 se os dados forem inválidos.
            return ResponseEntity.badRequest().build();
        }

        // Cadastra a mentoria usando o Service.
        Mentoria novaMentoria = mentoriaService.cadastrar(mentoria);

        // Retorna HTTP 201 com a mentoria cadastrada.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novaMentoria);
    }

    // Recebe uma requisição GET para listar todas as mentorias.
    @GetMapping
    public ResponseEntity<List<Mentoria>> listarMentorias() {

        // Busca todas as mentorias cadastradas no Service.
        List<Mentoria> mentorias = mentoriaService.listarTodos();

        // Retorna HTTP 200 com a lista de mentorias.
        return ResponseEntity.ok(mentorias);
    }


    // Recebe uma requisição GET para buscar uma mentoria pelo ID.
    @GetMapping("/{id}")
    public ResponseEntity<Mentoria> buscarMentoriaPorId(
            @PathVariable Long id) {

        // Procura a mentoria pelo ID informado.
        Mentoria mentoria = mentoriaService.buscarPorId(id);

        // Retorna HTTP 404 se a mentoria não existir.
        if (mentoria == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com a mentoria encontrada.
        return ResponseEntity.ok(mentoria);
    }


    // Recebe uma requisição PUT para atualizar uma mentoria.
    @PutMapping("/{id}")
    public ResponseEntity<Mentoria> atualizarMentoria(
            @PathVariable Long id,
            @RequestBody Mentoria dadosAtualizados) {

        // Verifica se a mentoria existe.
        if (mentoriaService.buscarPorId(id) == null) {

            // Retorna HTTP 404 se não encontrar a mentoria.
            return ResponseEntity.notFound().build();
        }

        // Verifica se os novos dados são válidos.
        if (!mentoriaService.dadosValidos(dadosAtualizados)) {

            // Retorna HTTP 400 se os dados forem inválidos.
            return ResponseEntity.badRequest().build();
        }

        // Atualiza a mentoria usando o Service.
        Mentoria mentoriaAtualizada =
                mentoriaService.atualizar(id, dadosAtualizados);

        // Retorna HTTP 200 com os dados atualizados.
        return ResponseEntity.ok(mentoriaAtualizada);
    }


    // Recebe uma requisição DELETE para excluir uma mentoria.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirMentoria(
            @PathVariable Long id) {

        // Solicita ao Service a exclusão da mentoria.
        boolean excluida = mentoriaService.excluir(id);

        // Retorna HTTP 404 se a mentoria não existir.
        if (!excluida) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 204 quando a exclusão é realizada.
        return ResponseEntity.noContent().build();
    }

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

