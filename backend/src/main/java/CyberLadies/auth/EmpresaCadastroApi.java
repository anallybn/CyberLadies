package CyberLadies.auth;
import CyberLadies.auth.dto.ContaDados.CadastroEmpresa;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
class EmpresaCadastroApi {
    private final EmpresaCadastroService service;
    EmpresaCadastroApi(EmpresaCadastroService service) { this.service=service; }
    @PostMapping("/api/auth/cadastro/empresa") @ResponseStatus(HttpStatus.CREATED)
    public void cadastrar(@Valid @RequestBody CadastroEmpresa dados) { service.cadastrar(dados); }
}
