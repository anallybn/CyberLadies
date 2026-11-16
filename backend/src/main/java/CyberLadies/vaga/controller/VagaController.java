package CyberLadies.vaga.controller;
import CyberLadies.vaga.model.Vaga;
import CyberLadies.vaga.service.VagaService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/vagas")
public class VagaController {
    private final VagaService service;
    public VagaController(VagaService service) { this.service = service; }
    @GetMapping
    public VagaService.Resultado listar(@RequestParam(defaultValue="") String busca,
            @RequestParam(defaultValue="") List<String> nivel,
            @RequestParam(defaultValue="") List<String> modalidade,
            @RequestParam(required=false) BigDecimal salarioMinimo,
            @RequestParam(defaultValue="false") boolean paraMaes,
            @RequestParam(defaultValue="false") boolean pcd,
            @RequestParam(defaultValue="false") boolean semFormacao) {
        return service.listar(busca,nivel,modalidade,salarioMinimo,paraMaes,pcd,semFormacao);
    }
    @GetMapping("/{id}")
    public Vaga buscar(@PathVariable long id) { return service.buscarPorId(id); }
    @RequestMapping(path={"","/{id}"}, method={RequestMethod.POST,RequestMethod.PUT,RequestMethod.PATCH,RequestMethod.DELETE})
    public void bloquearEscrita() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vagas são mantidas somente por carga administrativa.");
    }
}