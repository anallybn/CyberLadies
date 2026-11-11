package CyberLadies.agenda.controller;

import CyberLadies.agenda.model.EventoAgenda;


import CyberLadies.agenda.service.EventoAgendaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
