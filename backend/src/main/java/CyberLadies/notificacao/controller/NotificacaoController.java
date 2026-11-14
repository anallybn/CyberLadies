package CyberLadies.notificacao.controller;

import CyberLadies.notificacao.model.Notificacao;

import CyberLadies.notificacao.service.NotificacaoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController

@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    // Serviço responsável pelas notificações.
    private final NotificacaoService notificacaoService;

    // O Spring fornece automaticamente o serviço.
    public NotificacaoController(
            NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    // Recebe uma notificação e verifica se está marcada como lida.
    @PostMapping("/verificar-leitura")
    public ResponseEntity<String> verificarLeitura(
            @RequestBody Notificacao notificacao) {

        // Consulta o estado de leitura usando o serviço.
        if (notificacaoService.estaLida(notificacao)) {
            return ResponseEntity.ok(
                    "Notificação marcada como lida."
            );
        }

        // Retorna o estado quando não está marcada como lida.
        return ResponseEntity.ok(
                "Notificação não lida."
        );
    }
}
