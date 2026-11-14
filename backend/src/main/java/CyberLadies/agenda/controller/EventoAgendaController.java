package CyberLadies.agenda.controller;

import CyberLadies.agenda.model.EventoAgenda;
import CyberLadies.agenda.service.EventoAgendaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List; // Permite retornar uma lista de eventos.

@RestController

@RequestMapping("/api/agenda")
public class EventoAgendaController {

    // Serviço responsável pelas regras da agenda.
    private final EventoAgendaService eventoAgendaService;

    // O Spring fornece automaticamente o serviço.
    public EventoAgendaController(
            EventoAgendaService eventoAgendaService) {
        this.eventoAgendaService = eventoAgendaService;
    }

    // Recebe um evento e verifica se ele já terminou.
    @PostMapping("/verificar-termino")
    public ResponseEntity<String> verificarTermino(
            @RequestBody EventoAgenda evento) {

        // Consulta o horário de término usando o serviço.
        if (eventoAgendaService.terminou(evento)) {
            return ResponseEntity.ok(
                    "O evento já terminou."
            );
        }

        // Retorna a informação quando o evento não terminou.
        return ResponseEntity.ok(
                "O evento ainda não terminou."
        );
    }

    // Recebe os dados de um novo evento.
    @PostMapping
    public ResponseEntity<EventoAgenda> cadastrar(
            @RequestBody EventoAgenda dados) {

        // Envia os dados ao Service para validar e cadastrar.
        EventoAgenda novoEvento = eventoAgendaService.cadastrar(dados);

        // Retorna HTTP 201 com o evento cadastrado.
        return ResponseEntity.status(201).body(novoEvento);
    }

    // Retorna todos os eventos cadastrados na agenda.
    @GetMapping
    public ResponseEntity<List<EventoAgenda>> listarTodos() {

        // Busca os eventos armazenados no Service.
        List<EventoAgenda> eventos = eventoAgendaService.listarTodos();

        // Retorna HTTP 200 com a lista de eventos.
        return ResponseEntity.ok(eventos);
    }

    // Busca um evento pelo seu identificador.
    @GetMapping("/{id}")
    public ResponseEntity<EventoAgenda> buscarPorId(
            @PathVariable Long id) {

        // Solicita ao Service o evento correspondente.
        EventoAgenda evento = eventoAgendaService.buscarPorId(id);

        // Retorna HTTP 404 caso o evento nao exista.
        if (evento == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com o evento encontrado.
        return ResponseEntity.ok(evento);
    }

    // Atualiza um evento existente pelo ID.
    @PutMapping("/{id}")
    public ResponseEntity<EventoAgenda> atualizar(
            @PathVariable Long id,
            @RequestBody EventoAgenda dados) {

        // Envia os novos dados ao Service.
        EventoAgenda eventoAtualizado =
                eventoAgendaService.atualizar(id, dados);

        // Retorna HTTP 404 se o evento nao existir.
        if (eventoAtualizado == null) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 200 com o evento atualizado.
        return ResponseEntity.ok(eventoAtualizado);
    }

    // Exclui um evento da agenda pelo ID.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id) {

        // Solicita ao Service a exclusao do evento.
        boolean excluido = eventoAgendaService.excluir(id);

        // Retorna HTTP 404 se o evento nao existir.
        if (!excluido) {
            return ResponseEntity.notFound().build();
        }

        // Retorna HTTP 204 quando a exclusao funciona.
        return ResponseEntity.noContent().build();
    }

}
