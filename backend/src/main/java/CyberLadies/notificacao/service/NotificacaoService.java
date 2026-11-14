package CyberLadies.notificacao.service;

import CyberLadies.notificacao.model.Notificacao;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

// Identifica esta classe como um serviço do Spring Boot.
@Service
public class NotificacaoService {

    // Cria uma nova notificação para uma usuária.
    public Notificacao criar(Long usuariaId, String titulo, String mensagem) {

        // Verifica se a usuária foi identificada corretamente.
        if (usuariaId == null || usuariaId <= 0) {
            throw new IllegalArgumentException("Usuária inválida.");
        }

        // O título da notificação é obrigatório.
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException(
                "Informe o título da notificação."
            );
        }

        // A mensagem também é obrigatória.
        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException(
                "Informe a mensagem da notificação."
            );
        }

        // Remove espaços desnecessários.
        String tituloFormatado = titulo.strip();
        String mensagemFormatada = mensagem.strip();

        // Limita o tamanho do título.
        if (tituloFormatado.length() > 120) {
            throw new IllegalArgumentException(
                "O título deve ter no máximo 120 caracteres."
            );
        }

        // Limita o tamanho da mensagem.
        if (mensagemFormatada.length() > 500) {
            throw new IllegalArgumentException(
                "A mensagem deve ter no máximo 500 caracteres."
            );
        }

        // Cria a notificação inicialmente como não lida.
        return new Notificacao(
            null,
            usuariaId,
            tituloFormatado,
            mensagemFormatada,
            false,
            LocalDateTime.now()
        );
    }

    // Marca uma notificação como lida.
    public void marcarComoLida(Notificacao notificacao) {

        // Impede alterações em uma notificação inexistente.
        if (notificacao == null) {
            throw new IllegalArgumentException(
                "Notificação não encontrada."
            );
        }

        // Atualiza o estado da notificação.
        notificacao.setLida(true);
    }

    // Verifica se uma notificação já foi lida.
    public boolean estaLida(Notificacao notificacao) {
        return notificacao != null && notificacao.isLida();
    }
}
