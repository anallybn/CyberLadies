package CyberLadies.academy.controller;
import CyberLadies.academy.dto.AlunasDados.*;
import CyberLadies.academy.service.AlunasService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/educadora/alunas")
public class AlunasController {
    private final AlunasService service;
    public AlunasController(AlunasService service){this.service=service;}
    @GetMapping public ResponseEntity<Painel> painel(@RequestParam(defaultValue="")String busca,
        @RequestParam(defaultValue="TODAS")String status,@RequestParam(required=false)Long trilhaId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.painel(busca,status,trilhaId));
    }
    @GetMapping("/{alunaId}/trilhas/{trilhaId}/progresso")
    public ResponseEntity<Detalhe> detalhe(@PathVariable long alunaId,@PathVariable long trilhaId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detalhe(alunaId,trilhaId));
    }
}
