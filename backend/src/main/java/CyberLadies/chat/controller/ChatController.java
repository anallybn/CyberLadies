package CyberLadies.chat.controller;
import CyberLadies.chat.dto.ChatDados.*;
import CyberLadies.chat.service.ChatService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/chat")
public class ChatController {
 private final ChatService service;
 public ChatController(ChatService service){this.service=service;}
 private <T> ResponseEntity<T> resposta(T dados){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);}
 @GetMapping("/contextos") public ResponseEntity<Contextos> contextos(@RequestParam(defaultValue="0")int pagina){return resposta(service.contextos(pagina));}
 @PostMapping("/conversas") public ResponseEntity<Conversa> criar(@Valid @RequestBody Criacao dados){return resposta(service.criar(dados));}
 @GetMapping("/conversas") public ResponseEntity<Conversas> listar(@RequestParam(defaultValue="")String busca,@RequestParam(defaultValue="0")int pagina){return resposta(service.listar(busca,pagina));}
 @GetMapping("/conversas/{id}") public ResponseEntity<Conversa> detalhe(@PathVariable UUID id){return resposta(service.detalhe(id));}
 @GetMapping("/conversas/{id}/mensagens") public ResponseEntity<Historico> mensagens(@PathVariable UUID id,@RequestParam(required=false)Long antes,@RequestParam(required=false)Long apos){return resposta(service.historico(id,antes,apos));}
 @PostMapping("/conversas/{id}/mensagens") public ResponseEntity<Mensagem> enviar(@PathVariable UUID id,@Valid @RequestBody Envio dados){return resposta(service.enviar(id,dados));}
 @PutMapping("/conversas/{id}/leitura") public ResponseEntity<Void> ler(@PathVariable UUID id,@Valid @RequestBody Leitura dados){service.ler(id,dados);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
}
