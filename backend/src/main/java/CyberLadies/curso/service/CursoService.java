package CyberLadies.curso.service;

import CyberLadies.curso.model.Curso;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class CursoService {
    // Armazena temporariamente os cursos cadastrados.
    private final List<Curso> cursos = new ArrayList<>();

    // Gera identificadores únicos para os novos cursos.
    private final AtomicLong proximoId = new AtomicLong(1);

    // Cadastra um novo curso na memória.
    public synchronized Curso cadastrar(Curso curso) {

        // Verifica se os dados do curso são válidos.
        if (!dadosValidos(curso)) {
            throw new IllegalArgumentException(
                "Dados do curso inválidos."
            );
        }

        // Gera um identificador único para o curso.
        Long id = proximoId.getAndIncrement();

        // Define o identificador no curso.
        curso.setId(id);

        // Adiciona o curso à lista temporária.
        cursos.add(curso);

        // Retorna o curso cadastrado.
        return curso;
    }

    // Retorna todos os cursos cadastrados.
    public synchronized List<Curso> listarTodos() {

        // Cria uma cópia da lista para proteger os dados.
        return new ArrayList<>(cursos);
    }


    // Busca um curso pelo seu identificador.
    public synchronized Curso buscarPorId(Long id) {

        // Verifica se o identificador é válido.
        if (id == null || id <= 0) {
            return null;
        }

        // Percorre todos os cursos cadastrados.
        for (Curso curso : cursos) {

            // Compara o ID do curso com o ID procurado.
            if (id.equals(curso.getId())) {

                // Retorna o curso encontrado.
                return curso;
            }
        }

        // Retorna null quando o curso não existe.
        return null;
    }


    // Atualiza os dados de um curso existente.
    public synchronized Curso atualizar(
            Long id, Curso dadosAtualizados) {

        // Busca o curso pelo identificador.
        Curso cursoExistente = buscarPorId(id);

        // Verifica se o curso foi encontrado.
        if (cursoExistente == null) {
            throw new IllegalArgumentException(
                "Curso não encontrado."
            );
        }

        // Valida os novos dados antes de atualizar.
        if (!dadosValidos(dadosAtualizados)) {
            throw new IllegalArgumentException(
                "Dados do curso inválidos."
            );
        }

        // Atualiza a educadora responsável.
        cursoExistente.setEducadoraId(
            dadosAtualizados.getEducadoraId()
        );

        // Atualiza o título do curso.
        cursoExistente.setTitulo(
            dadosAtualizados.getTitulo()
        );

        // Atualiza a descrição do curso.
        cursoExistente.setDescricao(
            dadosAtualizados.getDescricao()
        );

        // Atualiza a categoria do curso.
        cursoExistente.setCategoria(
            dadosAtualizados.getCategoria()
        );

        // Atualiza a carga horária.
        cursoExistente.setCargaHoraria(
            dadosAtualizados.getCargaHoraria()
        );

        // Retorna o curso com os dados atualizados.
        return cursoExistente;
    }


    // Exclui um curso pelo seu identificador.
    public synchronized boolean excluir(Long id) {

        // Busca o curso que será excluído.
        Curso curso = buscarPorId(id);

        // Verifica se o curso existe.
        if (curso == null) {

            // Retorna falso quando não encontra o curso.
            return false;
        }

        // Remove o curso da lista temporária.
        cursos.remove(curso);

        // Confirma que o curso foi excluído.
        return true;
    }

    // Verifica se os dados obrigatórios do curso são válidos.
    public boolean dadosValidos(Curso curso) {

        // Impede a validação de um curso inexistente.
        if (curso == null) {
            return false;
        }

        // Confere os campos obrigatórios do curso.
        return curso.getEducadoraId() != null
                && curso.getTitulo() != null
                && !curso.getTitulo().isBlank()
                && curso.getDescricao() != null
                && !curso.getDescricao().isBlank()
                && curso.getCategoria() != null
                && !curso.getCategoria().isBlank()
                && curso.getCargaHoraria() > 0;
    }
}
