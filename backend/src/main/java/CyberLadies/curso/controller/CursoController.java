package CyberLadies.curso.controller;

import java.util.List;

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


    // Recebe uma requisição POST para cadastrar um curso.
    @PostMapping
    public ResponseEntity<Curso> cadastrarCurso(
            @RequestBody Curso curso) {

        // Verifica se os dados enviados são válidos.
        if (!cursoService.dadosValidos(curso)) {

            // Retorna HTTP 400 quando os dados são inválidos.
            return ResponseEntity.badRequest().build();
        }

        // Cadastra o curso utilizando o Service.
        Curso cursoCadastrado = cursoService.cadastrar(curso);

        // Retorna HTTP 201 com os dados do curso criado.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cursoCadastrado);
    }

    // Recebe uma requisição GET para listar todos os cursos.
    @GetMapping
    public ResponseEntity<List<Curso>> listarCursos() {

        // Busca todos os cursos cadastrados no Service.
        List<Curso> cursos = cursoService.listarTodos();

        // Retorna HTTP 200 com a lista de cursos.
        return ResponseEntity.ok(cursos);
    }


    // Recebe uma requisição GET para buscar um curso pelo ID.
    @GetMapping("/{id}")
    public ResponseEntity<Curso> buscarCursoPorId(
            @PathVariable Long id) {

        // Busca o curso utilizando o Service.
        Curso curso = cursoService.buscarPorId(id);

        // Verifica se o curso foi encontrado.
        if (curso == null) {

            // Retorna HTTP 404 quando o curso não existe.
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com os dados do curso.
        return ResponseEntity.ok(curso);
    }


    // Recebe uma requisição PUT para atualizar um curso.
    @PutMapping("/{id}")
    public ResponseEntity<Curso> atualizarCurso(
            @PathVariable Long id,
            @RequestBody Curso dadosAtualizados) {

        // Busca o curso que será atualizado.
        Curso cursoExistente = cursoService.buscarPorId(id);

        // Retorna HTTP 404 se o curso não existir.
        if (cursoExistente == null) {
            return ResponseEntity.notFound().build();
        }

        // Verifica se os novos dados são válidos.
        if (!cursoService.dadosValidos(dadosAtualizados)) {
            return ResponseEntity.badRequest().build();
        }

        // Atualiza o curso utilizando o Service.
        Curso cursoAtualizado = cursoService.atualizar(
                id, dadosAtualizados
        );

        // Retorna HTTP 200 com os dados atualizados.
        return ResponseEntity.ok(cursoAtualizado);
    }


    // Recebe uma requisição DELETE para excluir um curso.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCurso(
            @PathVariable Long id) {

        // Solicita a exclusão do curso ao Service.
        boolean excluido = cursoService.excluir(id);

        // Verifica se o curso foi encontrado e excluído.
        if (!excluido) {

            // Retorna HTTP 404 quando o curso não existe.
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 204 quando a exclusão é concluída.
        return ResponseEntity.noContent().build();
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