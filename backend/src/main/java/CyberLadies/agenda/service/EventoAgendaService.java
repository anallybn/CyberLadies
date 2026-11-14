package CyberLadies.agenda.service;

import CyberLadies.agenda.model.EventoAgenda;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;

import java.util.ArrayList; // Permite armazenar eventos em uma lista.
import java.util.List; // Representa uma lista de eventos.
import java.util.concurrent.atomic.AtomicLong; // Gera IDs sequenciais.

@Service
public class EventoAgendaService {
    // Armazena temporariamente os eventos cadastrados em memoria.
    private final List<EventoAgenda> eventos = new ArrayList<>();

    // Gera um identificador unico para cada evento.
    private final AtomicLong proximoId = new AtomicLong(1);

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

    // Cadastra um novo evento na agenda.
    public synchronized EventoAgenda cadastrar(EventoAgenda dados) {

        // Verifica se os dados foram enviados.
        if (dados == null) {
            throw new IllegalArgumentException(
                "Informe os dados do evento."
            );
        }

        // Reutiliza as validacoes existentes no metodo criar().
        EventoAgenda evento = criar(
            dados.getUsuariaId(),
            dados.getTitulo(),
            dados.getDescricao(),
            dados.getTipoEvento(),
            dados.getDataInicio(),
            dados.getDataFim()
        );

        // Gera um ID unico para o novo evento.
        evento.setId(proximoId.getAndIncrement());

        // Armazena o evento temporariamente em memoria.
        eventos.add(evento);

        // Retorna o evento cadastrado.
        return evento;
    }

    // Retorna todos os eventos cadastrados na agenda.
    public synchronized List<EventoAgenda> listarTodos() {

        // Cria uma copia da lista de eventos.
        // Isso evita que outras partes do sistema alterem
        // diretamente a lista original.
        return new ArrayList<>(eventos);
    }


    // Busca um evento pelo seu identificador.
    public synchronized EventoAgenda buscarPorId(Long id) {

        // Verifica se o ID informado e valido.
        if (id == null || id <= 0) {
            return null;
        }

        // Percorre todos os eventos cadastrados.
        for (EventoAgenda evento : eventos) {

            // Compara o ID do evento com o ID solicitado.
            if (evento.getId().equals(id)) {

                // Retorna o evento encontrado.
                return evento;
            }
        }

        // Retorna null caso o evento nao exista.
        return null;
    }

    // Atualiza um evento existente na agenda.
    public synchronized EventoAgenda atualizar(Long id, EventoAgenda dados) {

        // Procura o evento pelo ID informado.
        EventoAgenda existente = buscarPorId(id);

        // Retorna null se o evento nao existir.
        if (existente == null) {
            return null;
        }

        // Verifica se os novos dados foram enviados.
        if (dados == null) {
            throw new IllegalArgumentException(
                "Informe os dados do evento."
            );
        }

        // Reutiliza as validacoes do metodo criar().
        EventoAgenda validado = criar(
            dados.getUsuariaId(),
            dados.getTitulo(),
            dados.getDescricao(),
            dados.getTipoEvento(),
            dados.getDataInicio(),
            dados.getDataFim()
        );

        // Atualiza os campos do evento existente.
        existente.setUsuariaId(validado.getUsuariaId());
        existente.setTitulo(validado.getTitulo());
        existente.setDescricao(validado.getDescricao());
        existente.setTipoEvento(validado.getTipoEvento());
        existente.setDataInicio(validado.getDataInicio());
        existente.setDataFim(validado.getDataFim());

        // Preserva o ID original do evento.
        return existente;
    }
 
    // Exclui um evento cadastrado na agenda.
    public synchronized boolean excluir(Long id) {

        // Procura o evento pelo identificador informado.
        EventoAgenda existente = buscarPorId(id);

        // Retorna false se o evento nao existir.
        if (existente == null) {
            return false;
        }

        // Remove o evento da lista em memoria.
        eventos.remove(existente);

        // Retorna true para confirmar a exclusao.
        return true;
    }

}
