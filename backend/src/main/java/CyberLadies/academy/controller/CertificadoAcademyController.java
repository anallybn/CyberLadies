package CyberLadies.academy.controller;

import CyberLadies.academy.dto.AcademyDados.Certificado;
import CyberLadies.academy.service.CertificadoAcademyService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/academy/me/certificados")
public class CertificadoAcademyController {
    private final CertificadoAcademyService service;
    public CertificadoAcademyController(CertificadoAcademyService service) { this.service=service; }
    @GetMapping public ResponseEntity<List<Certificado>> meus() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.meus());
    }
    @GetMapping("/{id}") public ResponseEntity<Certificado> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buscar(id));
    }
}