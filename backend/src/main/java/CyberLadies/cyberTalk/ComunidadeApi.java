package CyberLadies.cyberTalk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

/** Adaptador HTTP: entrada, status e cabe?alhos; regras ficam no servi?o. */
@RestController
@RequestMapping("/api")
class ComunidadeApi {
    private final ComunidadeService service;
    ComunidadeApi(ComunidadeService service) { this.service=service; }

    @GetMapping("/usuaria")
    AcessoTalk.Usuaria usuaria() { return service.usuaria(); }
    @PutMapping("/comunidade/adesao") @ResponseStatus(HttpStatus.NO_CONTENT)
    void aderir() { service.aderir(); }
    @GetMapping("/posts")
    List<PublicacaoResponse> listar() { return service.listar(); }
    @GetMapping("/posts/{id}")
    PublicacaoResponse consultar(@PathVariable long id) { return service.consultar(id); }
    @PostMapping(value="/posts",consumes=MediaType.APPLICATION_JSON_VALUE) @ResponseStatus(HttpStatus.CREATED)
    PublicacaoResponse publicar(@Valid @RequestBody TextoPublicacao entrada) { return service.publicar(entrada.texto()); }
    @PostMapping(value="/posts",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    PublicacaoResponse publicarComAnexo(@RequestParam(defaultValue="") String texto,@RequestParam MultipartFile arquivo) throws IOException {
        return service.publicarComAnexo(texto,arquivo);
    }
    @GetMapping("/posts/{id}/anexo")
    ResponseEntity<byte[]> anexo(@PathVariable long id) {
        var anexo=service.anexo(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(anexo.tipo()))
            .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment()
                .filename(anexo.nome(),StandardCharsets.UTF_8).build().toString()).body(anexo.dados());
    }
    @PutMapping("/posts/{id}")
    PublicacaoResponse editar(@PathVariable long id,@Valid @RequestBody TextoPublicacao entrada) { return service.editar(id,entrada.texto()); }
    @DeleteMapping("/posts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void excluir(@PathVariable long id) { service.excluir(id); }
    @PutMapping("/posts/{id}/curtida")
    PublicacaoResponse curtir(@PathVariable long id) { return service.curtir(id); }
    @DeleteMapping("/posts/{id}/curtida")
    PublicacaoResponse descurtir(@PathVariable long id) { return service.descurtir(id); }
    @GetMapping("/posts/{id}/comentarios")
    List<ComentarioResponse> comentarios(@PathVariable long id) { return service.comentarios(id); }
    @PostMapping("/posts/{id}/comentarios") @ResponseStatus(HttpStatus.CREATED)
    ComentarioResponse comentar(@PathVariable long id,@Valid @RequestBody TextoPublicacao entrada) { return service.comentar(id,entrada.texto()); }
}
