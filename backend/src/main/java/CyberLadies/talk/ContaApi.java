package CyberLadies.talk;

import CyberLadies.auth.dto.ContaDados.*;
import CyberLadies.auth.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class ContaApi {
    private final ContaService service;
    private final AcessoConta acesso;
    ContaApi(ContaService service,AcessoConta acesso) { this.service=service; this.acesso=acesso; }
    @PostMapping("/auth/cadastro/usuaria") @ResponseStatus(HttpStatus.CREATED)
    void cadastrar(@Valid @RequestBody CadastroUsuaria dados) { service.cadastrar(dados); }
    @PostMapping("/auth/cadastro/educadora") @ResponseStatus(HttpStatus.CREATED)
    void cadastrarEducadora(@Valid @RequestBody CadastroEducadora dados) { service.cadastrar(dados); }
    @PostMapping("/auth/cadastro/empresa") @ResponseStatus(HttpStatus.CREATED)
    void cadastrarEmpresa(@Valid @RequestBody CadastroEmpresa dados) { service.cadastrar(dados); }
    @PostMapping("/auth/login")
    ResponseEntity<Sessao> entrar(@Valid @RequestBody Login dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.entrar(dados));
    }
    @GetMapping("/auth/conta")
    ResponseEntity<Conta> conta() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(acesso.conta()); }
    @DeleteMapping("/auth/sessao") @ResponseStatus(HttpStatus.NO_CONTENT)
    void sair(@RequestHeader(value="Authorization",defaultValue="") String header) { service.sair(header); }
    static String hashBearer(String header) { return Credenciais.hashBearer(header); }
}