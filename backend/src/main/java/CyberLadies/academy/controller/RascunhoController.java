package CyberLadies.academy.controller;

import CyberLadies.academy.dto.RascunhoDados.*;
import CyberLadies.academy.service.RascunhoService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/educadora/materiais/rascunhos")
public class RascunhoController {
    private final RascunhoService service;
    public RascunhoController(RascunhoService service) { this.service=service; }
    @GetMapping public ResponseEntity<List<Resumo>> listar() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar()); }
    @GetMapping("/{id}") public ResponseEntity<Rascunho> buscar(@PathVariable UUID id) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buscar(id)); }
    @PostMapping public ResponseEntity<Rascunho> criar(@Valid @RequestBody Entrada entrada,@RequestHeader("Idempotency-Key") UUID chave) {
        var salvo=service.criar(entrada,chave);
        return ResponseEntity.created(java.net.URI.create("/api/educadora/materiais/rascunhos/"+salvo.id())).cacheControl(CacheControl.noStore()).body(salvo);
    }
    @PutMapping("/{id}") public ResponseEntity<Rascunho> atualizar(@PathVariable UUID id,@Valid @RequestBody Entrada entrada,@RequestHeader("Idempotency-Key") UUID operacao) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.atualizar(id,entrada,operacao));
    }
}
