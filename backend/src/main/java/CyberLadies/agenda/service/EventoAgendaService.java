package CyberLadies.agenda.service;

import CyberLadies.agenda.model.EventoAgenda;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;


@Service
public class EventoAgendaService {

    // Cria um evento na agenda de uma usuária.
    public EventoAgenda criar(Long usuariaId, String titulo, String descricao, String tipoEvento, LocalDateTime dataInicio, LocalDateTime dataFim) {

        // Verifica se o identificador da usuária é válido.
        if (usuariaId == null || usuariaId <= 0) {
            throw new IllegalArgumentException(
                "Usuária inválida."
            );
        }

        // O título do evento é obrigatório.
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException(
                "Informe o título do evento."
            );
        }

        // Remove espaços extras do título.
        String tituloFormatado = titulo.strip();

        // Limita o tamanho do título.
        if (tituloFormatado.length() > 120) {
            throw new IllegalArgumentException(
                "O título deve ter no máximo 120 caracteres."
            );
        }

        // O tipo do evento é obrigatório.
        if (tipoEvento == null || tipoEvento.isBlank()) {
            throw new IllegalArgumentException(
                "Informe o tipo do evento."
            );
        }

        // Padroniza o tipo em letras maiúsculas.
        String tipo = tipoEvento.strip()
            .toUpperCase(Locale.ROOT);

        // Aceita apenas os tipos definidos para a agenda.
        if (!tipo.equals("CURSO")
                && !tipo.equals("MENTORIA")
                && !tipo.equals("OUTRO")) {
            throw new IllegalArgumentException(
                "O tipo deve ser CURSO, MENTORIA ou OUTRO."
            );
        }

        // Verifica se as datas foram informadas.
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException(
                "Informe o início e o término do evento."
            );
        }

        // O término precisa ser posterior ao início.
        if (!dataFim.isAfter(dataInicio)) {
            throw new IllegalArgumentException(
                "O término deve ser posterior ao início."
            );
        }

        // A descrição é opcional.
        String texto = descricao == null
            ? "" : descricao.strip();

        // Limita o tamanho da descrição.
        if (texto.length() > 500) {
            throw new IllegalArgumentException(
                "A descrição deve ter no máximo 500 caracteres."
            );
        }

        return new EventoAgenda(
            null,
            usuariaId,
            tituloFormatado,
            texto,
            tipo,
            dataInicio,
            dataFim
        );
    }

    // Verifica se um evento já terminou.
    public boolean terminou(EventoAgenda evento) {

        // Impede a consulta de um evento inexistente.
        if (evento == null || evento.getDataFim() == null) {
            return false;
        }

        // Compara o término com o horário atual.
        return !evento.getDataFim().isAfter(
            LocalDateTime.now()
        );
    }
}
