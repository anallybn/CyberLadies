package CyberLadies.cyberTalk;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
/** Proje??o comunit?ria, sem reutilizar a resposta do perfil privado. */
@RestController
@RequestMapping("/api/comunidade/perfis")
class PerfilAutoraApi {
    private final PerfilAutoraService service;
    PerfilAutoraApi(PerfilAutoraService service) { this.service=service; }
    @GetMapping("/{id}")
    ResponseEntity<PerfilAutoraResponse> consultar(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultar(id));
    }
    @GetMapping("/{id}/publicacoes")
    ResponseEntity<PublicacoesAutoraResponse> publicacoes(@PathVariable long id,
        @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="10") int tamanho) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicacoes(id,pagina,tamanho));
    }
}
