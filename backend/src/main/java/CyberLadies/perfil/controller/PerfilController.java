package CyberLadies.perfil.controller;

import CyberLadies.perfil.dto.PerfilDados.*;
import CyberLadies.perfil.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {
    private final PerfilService service;
    public PerfilController(PerfilService service) { this.service=service; }
    @GetMapping public ResponseEntity<Perfil> consultar() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultar());
    }
    @PutMapping public Pessoal editar(@Valid @RequestBody Edicao dados) { return service.editar(dados); }
    @PostMapping("/certificacoes") @ResponseStatus(HttpStatus.CREATED)
    public Certificacao adicionarCertificacao(@Valid @RequestBody CertificacaoEntrada dados) { return service.adicionarCertificacao(dados); }
    @PutMapping("/certificacoes/{id}")
    public Certificacao editarCertificacao(@PathVariable long id,@Valid @RequestBody CertificacaoEntrada dados) { return service.editarCertificacao(id,dados); }
    @DeleteMapping("/certificacoes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirCertificacao(@PathVariable long id) { service.excluirCertificacao(id); }
    @PostMapping("/experiencias") @ResponseStatus(HttpStatus.CREATED)
    public Experiencia adicionarExperiencia(@Valid @RequestBody ExperienciaEntrada dados) { return service.adicionarExperiencia(dados); }
    @PutMapping("/experiencias/{id}")
    public Experiencia editarExperiencia(@PathVariable long id,@Valid @RequestBody ExperienciaEntrada dados) { return service.editarExperiencia(id,dados); }
    @DeleteMapping("/experiencias/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirExperiencia(@PathVariable long id) { service.excluirExperiencia(id); }
    @GetMapping("/foto")
    public ResponseEntity<byte[]> foto() {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options","nosniff").body(service.foto());
    }
    @PutMapping(value="/foto",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salvarFoto(@RequestParam MultipartFile arquivo) { service.salvarFoto(arquivo); }
    @DeleteMapping("/foto") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirFoto() { service.excluirFoto(); }
}