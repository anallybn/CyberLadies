package CyberLadies.academy.controller;

import CyberLadies.academy.dto.AcademyDados.*;
import CyberLadies.academy.service.AcademyService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/academy")
public class AcademyController {
    private final AcademyService service;
    public AcademyController(AcademyService service) { this.service=service; }
    @GetMapping("/catalogo")
    public Catalogo catalogo(@RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="") String categoria,
        @RequestParam(defaultValue="") String nivel) { return service.catalogo(busca,categoria,nivel); }
    @GetMapping("/trilhas")
    public List<Trilha> trilhas(@RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="") String categoria,
        @RequestParam(defaultValue="") String nivel) { return service.catalogo(busca,categoria,nivel).trilhas(); }
    @GetMapping("/trilhas/{id}")
    public Trilha trilha(@PathVariable long id) { return service.trilha(id); }
    @GetMapping("/trilhas/{id}/aulas")
    public List<Aula> aulas(@PathVariable long id) { return service.aulas(id); }
    @GetMapping("/aulas")
    public List<Aula> aulasRecentes(@RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="") String categoria,
        @RequestParam(defaultValue="") String nivel) { return service.catalogo(busca,categoria,nivel).aulas(); }
    @GetMapping("/aulas/{id}")
    public org.springframework.http.ResponseEntity<AulaDetalhe> aula(@PathVariable long id) { return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(service.aula(id)); }
}