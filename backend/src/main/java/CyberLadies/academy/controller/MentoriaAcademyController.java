package CyberLadies.academy.controller;

import CyberLadies.academy.dto.AcademyDados.*;
import CyberLadies.academy.service.MentoriaAcademyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/academy")
public class MentoriaAcademyController {
    private final MentoriaAcademyService service;
    public MentoriaAcademyController(MentoriaAcademyService service) { this.service=service; }
    @GetMapping("/mentoras") public List<Mentora> mentoras() { return service.mentoras(); }
    @GetMapping("/mentoras/{id}/horarios") public List<Horario> horarios(@PathVariable long id) { return service.horarios(id); }
    @PostMapping("/agendamentos") @ResponseStatus(HttpStatus.CREATED)
    public Agendamento agendar(@Valid @RequestBody EntradaAgendamento entrada) { return service.agendar(entrada.disponibilidadeId()); }
    @GetMapping("/me/agendamentos")
    public ResponseEntity<List<Agendamento>> meus() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.meus()); }
    @DeleteMapping("/agendamentos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable long id) { service.cancelar(id); }
}