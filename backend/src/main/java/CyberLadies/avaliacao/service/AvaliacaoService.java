package CyberLadies.avaliacao.service;

import CyberLadies.avaliacao.model.Avaliacao;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;

import java.util.ArrayList; // Permite criar uma lista de avaliações.
import java.util.List; // Representa uma coleção de avaliações.
import java.util.concurrent.atomic.AtomicLong; // Gera IDs sequenciais.


@Service
public class AvaliacaoService {

    // Armazena temporariamente as avaliações cadastradas.
    private final List<Avaliacao> avaliacoes = new ArrayList<>();

    // Gera um ID único e sequencial para cada avaliação.
    private final AtomicLong proximoId = new AtomicLong(1);


    // Cria uma avaliação de curso ou mentoria.
    public Avaliacao criar(Long usuariaId, String tipoAtividade, Long atividadeId, Integer nota, String comentario) {

        // Verifica se a usuária foi identificada.
        if (usuariaId == null || usuariaId <= 0) {
            throw new IllegalArgumentException("Usuária inválida.");
        }

        // Verifica se a atividade existe como identificador.
        if (atividadeId == null || atividadeId <= 0) {
            throw new IllegalArgumentException("Atividade inválida.");
        }

        // Exige que o tipo de atividade seja informado.
        if (tipoAtividade == null || tipoAtividade.isBlank()) {
            throw new IllegalArgumentException(
                "Informe o tipo da atividade."
            );
        }

        // Padroniza o tipo de atividade em letras maiúsculas.
        String tipo = tipoAtividade.strip()
            .toUpperCase(Locale.ROOT);

        // Aceita apenas avaliações de cursos ou mentorias.
        if (!tipo.equals("CURSO") && !tipo.equals("MENTORIA")) {
            throw new IllegalArgumentException(
                "O tipo deve ser CURSO ou MENTORIA."
            );
        }

        // A nota deve estar entre 1 e 5 estrelas.
        if (nota == null || nota < 1 || nota > 5) {
            throw new IllegalArgumentException(
                "A nota deve estar entre 1 e 5."
            );
        }

        // O comentário é opcional.
        String texto = comentario == null ? "" : comentario.strip();

        // Limita o comentário a 500 caracteres.
        if (texto.length() > 500) {
            throw new IllegalArgumentException(
                "O comentário deve ter no máximo 500 caracteres."
            );
        }

        // Retorna uma avaliação com a data e hora atuais.
        return new Avaliacao(
            null,usuariaId, tipo, atividadeId, nota, texto,
            LocalDateTime.now()
        );
    }

    // Cadastra uma nova avaliação na lista em memória.
    public synchronized Avaliacao cadastrar(Avaliacao dados) {

        // Impede o cadastro sem os dados da avaliação.
        if (dados == null) {
            throw new IllegalArgumentException(
                "Informe os dados da avaliação."
            );
        }

        // Reutiliza as validações já existentes no projeto.
        Avaliacao avaliacao = criar(
            dados.getUsuariaId(),
            dados.getTipoAtividade(),
            dados.getAtividadeId(),
            dados.getNota(),
            dados.getComentario()
        );

        // Gera um identificador único para a avaliação.
        avaliacao.setId(proximoId.getAndIncrement());

        // Armazena a avaliação na lista temporária.
        avaliacoes.add(avaliacao);

        // Retorna a avaliação cadastrada.
        return avaliacao;
    }

    // Retorna todas as avaliações cadastradas em memória.
    public synchronized List<Avaliacao> listarTodos() {

        // Cria uma cópia da lista para proteger a lista original.
        return new ArrayList<>(avaliacoes);
    }

    // Busca uma avaliação pelo seu identificador.
    public synchronized Avaliacao buscarPorId(Long id) {

        // Percorre todas as avaliações cadastradas.
        for (Avaliacao avaliacao : avaliacoes) {

            // Verifica se o ID corresponde ao solicitado.
            if (avaliacao.getId().equals(id)) {

                // Retorna a avaliação encontrada.
                return avaliacao;
            }
        }

        // Retorna null quando a avaliação não existe.
        return null;
    }


    // Atualiza uma avaliação já cadastrada.
    public synchronized Avaliacao atualizar(Long id, Avaliacao dados) {

        // Busca a avaliação pelo ID informado.
        Avaliacao existente = buscarPorId(id);

        // Retorna null se a avaliação não existir.
        if (existente == null) {
            return null;
        }

        // Impede uma atualização sem dados.
        if (dados == null) {
            throw new IllegalArgumentException(
                "Informe os dados da avaliação."
            );
        }

        // Reutiliza as validações do método criar().
        Avaliacao validada = criar(
            dados.getUsuariaId(),
            dados.getTipoAtividade(),
            dados.getAtividadeId(),
            dados.getNota(),
            dados.getComentario()
        );

        // Atualiza os campos permitidos.
        existente.setUsuariaId(validada.getUsuariaId());
        existente.setTipoAtividade(validada.getTipoAtividade());
        existente.setAtividadeId(validada.getAtividadeId());
        existente.setNota(validada.getNota());
        existente.setComentario(validada.getComentario());

        // Mantém o ID e a data original da avaliação.
        return existente;
    }

    // Exclui uma avaliação cadastrada pelo ID.
    public synchronized boolean excluir(Long id) {

        // Busca a avaliação que será excluída.
        Avaliacao existente = buscarPorId(id);

        // Retorna false caso a avaliação não exista.
        if (existente == null) {
            return false;
        }

        // Remove a avaliação da lista em memória.
        avaliacoes.remove(existente);

        // Confirma que a exclusão foi realizada.
        return true;
    }

    // Verifica se a nota de uma avaliação é válida.
    public boolean notaValida(Integer nota) {
        return nota != null && nota >= 1 && nota <= 5;
    }
}
