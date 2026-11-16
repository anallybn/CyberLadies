package CyberLadies.academy.controller;

import CyberLadies.academy.dto.DashboardEducadoraDados.Dashboard;
import CyberLadies.academy.service.DashboardEducadoraService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/educadora")
public class DashboardEducadoraController {
    private final DashboardEducadoraService service;
    public DashboardEducadoraController(DashboardEducadoraService service) { this.service=service; }
    @GetMapping("/dashboard")
    public ResponseEntity<Dashboard> dashboard() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.dashboard());
    }
    // Não há painel editorial nem API de escrita nesta etapa, mesmo para aprovadas.
    @RequestMapping(value={"/trilhas","/aulas","/publicacoes","/trilhas/**","/aulas/**"},method={RequestMethod.POST,RequestMethod.PUT,RequestMethod.PATCH,RequestMethod.DELETE})
    public void escritaBloqueada() {
        service.dashboard(); // Autentica e exige o perfil real antes da recusa.
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Publicação indisponível nesta etapa.");
    }
}
