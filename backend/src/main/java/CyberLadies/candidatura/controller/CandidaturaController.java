package CyberLadies.candidatura.controller;
import CyberLadies.candidatura.model.Candidatura;
import CyberLadies.candidatura.service.CandidaturaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/candidaturas")
public class CandidaturaController {
    private final CandidaturaService service;
    public CandidaturaController(CandidaturaService service) { this.service=service; }
    public record Entrada(@NotNull @Positive Long vagaId) {}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Candidatura cadastrar(@Valid @RequestBody Entrada entrada) { return service.cadastrar(entrada.vagaId()); }
    @GetMapping
    public List<Candidatura> listar() { return service.listar(); }
    @GetMapping("/{id}")
    public Candidatura buscar(@PathVariable long id) { return service.buscar(id); }
    @RequestMapping(path={"","/{id}"},method={RequestMethod.PUT,RequestMethod.PATCH,RequestMethod.DELETE})
    public void bloquearAlteracao() { service.bloquearAlteracao(); }
}