package CyberLadies.auth;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcessoAutenticado {
    public record Identidade(long id, Set<Papel> papeis) {
        public Identidade {
            papeis = Collections.unmodifiableSet(papeis.isEmpty()
                ? EnumSet.noneOf(Papel.class) : EnumSet.copyOf(papeis));
        }

        public boolean possui(Papel papel) { return papeis.contains(papel); }
    }

    private final SessaoRepository sessoes;
    private final HttpServletRequest request;
    private final PapelRepository papeis;

    AcessoAutenticado(SessaoRepository sessoes, HttpServletRequest request, PapelRepository papeis) {
        this.sessoes = sessoes;
        this.request = request;
        this.papeis = papeis;
    }

    public Identidade identidade() {
        return sessoes.buscarConta(SessaoService.hashBearer(request.getHeader("Authorization")))
            .map(id -> new Identidade(id, papeis.buscar(id)))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada."));
    }
}
