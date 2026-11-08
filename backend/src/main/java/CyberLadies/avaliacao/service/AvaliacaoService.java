package CyberLadies.avaliacao.service;

import CyberLadies.avaliacao.model.Avaliacao;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AvaliacaoService {

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

    // Verifica se a nota de uma avaliação é válida.
    public boolean notaValida(Integer nota) {
        return nota != null && nota >= 1 && nota <= 5;
    }
}
