package CyberLadies.perfil.controller;

import CyberLadies.perfil.dto.PerfilDados.*;
import CyberLadies.perfil.service.PerfilService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
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
    @PostMapping(value="/certificacoes",consumes=MediaType.APPLICATION_JSON_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public Certificacao adicionarCertificacao(@Valid @RequestBody CertificacaoEntrada dados) {
        return service.adicionarCertificacao(dados,null);
    }
    @PostMapping(value="/certificacoes",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public Certificacao adicionarCertificacaoComPdf(@Valid @RequestPart CertificacaoEntrada dados,
            @RequestPart MultipartFile arquivo) {
        return service.adicionarCertificacao(dados,arquivo);
    }
    @PutMapping(value="/certificacoes/{id}",consumes=MediaType.APPLICATION_JSON_VALUE)
    public Certificacao editarCertificacao(@PathVariable long id, @Valid @RequestBody CertificacaoEntrada dados) {
        return service.editarCertificacao(id,dados,null,false);
    }
    @PutMapping(value="/certificacoes/{id}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Certificacao editarCertificacaoComPdf(@PathVariable long id, @Valid @RequestPart CertificacaoEntrada dados,
            @RequestPart(required=false) MultipartFile arquivo, @RequestParam(defaultValue="false") boolean removerPdf) {
        return service.editarCertificacao(id,dados,arquivo,removerPdf);
    }
    @GetMapping("/certificacoes/{id}/pdf")
    public ResponseEntity<byte[]> pdfCertificacao(@PathVariable long id, @RequestParam(defaultValue="false") boolean baixar) {
        var pdf=service.pdfCertificacao(id);
        var disposicao=baixar ? ContentDisposition.attachment() : ContentDisposition.inline();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION,disposicao.filename(pdf.nome(),StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options","nosniff")
            .header("Content-Security-Policy","sandbox; default-src 'none'")
            .body(pdf.dados());
    }
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