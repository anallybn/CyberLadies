package CyberLadies.chat.dto;
import CyberLadies.auth.dto.ContaDados.Estrita;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;
public final class ChatDados {
 private ChatDados() {}
 public record Criacao(@NotBlank @Size(max=100) String contexto) implements Estrita {}
 public record Envio(@NotBlank @Size(max=2000) String texto,@NotNull UUID chaveEnvio) implements Estrita {}
 public record Leitura(@Positive long sequencia) implements Estrita {}
 public record Contexto(String chave,String tipo,String titulo,String participante,String perfil) {}
 public record Contextos(List<Contexto> itens,boolean mais) {}
 public record Conversa(UUID id,String tipo,String titulo,String participante,String perfil,boolean podeEnviar,
  String motivoBloqueio,long naoLidas,OffsetDateTime atividadeEm) {}
 public record Conversas(List<Conversa> itens,boolean mais) {}
 public record Mensagem(UUID id,long sequencia,String texto,boolean propria,OffsetDateTime enviadoEm,UUID chaveEnvio) {}
 public record Historico(List<Mensagem> itens,boolean mais) {}
}
