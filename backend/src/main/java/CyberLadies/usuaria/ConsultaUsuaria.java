package CyberLadies.usuaria;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class ConsultaUsuaria {
    public record Dados(long id, String nome, String competencias) {}

    private final UsuariaRepository usuarias;

    ConsultaUsuaria(UsuariaRepository usuarias) { this.usuarias = usuarias; }

    public Optional<Dados> buscar(long id) {
        return usuarias.buscar(id);
    }
}
