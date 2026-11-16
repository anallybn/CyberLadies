package CyberLadies.academy.controller;

import CyberLadies.academy.dto.AcademyDados.*;
import CyberLadies.academy.service.EstudoService;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/academy")
public class EstudoController {
    private final EstudoService service;
    public EstudoController(EstudoService service) { this.service=service; }
    @PostMapping("/trilhas/{id}/inscricoes") @ResponseStatus(HttpStatus.CREATED)
    public Progresso inscrever(@PathVariable long id) { return service.inscrever(id); }
    @GetMapping("/me/inscricoes")
    public ResponseEntity<List<Inscricao>> inscricoes() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.inscricoes()); }
    @GetMapping("/me/progresso")
    public ResponseEntity<List<Progresso>> progressos() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.meusProgressos()); }
    @GetMapping("/trilhas/{id}/progresso")
    public ResponseEntity<Progresso> progresso(@PathVariable long id) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.progresso(id)); }
    @PutMapping("/aulas/{id}/conclusao")
    public Progresso concluir(@PathVariable long id) { return service.concluir(id); }
}