package CyberLadies.vaga.service;
import CyberLadies.vaga.model.Vaga;
import CyberLadies.vaga.repository.VagaRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class VagaService {
    private final VagaRepository repository;
    public VagaService(VagaRepository repository) { this.repository = repository; }
    public record Resultado(List<Vaga> vagas, long totalResultados, long vagasDisponiveis, long empresas) {}
    public Resultado listar(String busca, List<String> niveis, List<String> modalidades,
            BigDecimal salarioMinimo, boolean paraMaes, boolean pcd, boolean semFormacao) {
        validar(niveis, Set.of("ESTAGIO","JUNIOR","PLENO","SENIOR"));
        validar(modalidades, Set.of("REMOTO","HIBRIDO","PRESENCIAL"));
        if (busca.length() > 150 || salarioMinimo != null &&
            (salarioMinimo.signum() < 0 || salarioMinimo.compareTo(new BigDecimal("1000000")) > 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parâmetros de busca inválidos.");
        var todas = repository.listar();
        String termo = normalizar(busca.strip());
        var filtradas = todas.stream()
            .filter(v -> termo.isEmpty() || normalizar(v.titulo()+" "+v.empresa()+" "+v.descricao()+" "+v.requisitos()+" "+v.tecnologias()).contains(termo))
            .filter(v -> niveis.isEmpty() || niveis.contains(v.nivel()))
            .filter(v -> modalidades.isEmpty() || modalidades.contains(v.modalidade()))
            .filter(v -> salarioMinimo == null || (v.salarioMaximo() != null ?
                v.salarioMaximo().compareTo(salarioMinimo) >= 0 :
                v.salarioMinimo() != null && v.salarioMinimo().compareTo(salarioMinimo) >= 0))
            .filter(v -> !paraMaes || v.paraMaes()).filter(v -> !pcd || v.pcd())
            .filter(v -> !semFormacao || v.semFormacao()).toList();
        return new Resultado(filtradas, filtradas.size(), todas.size(),
            todas.stream().map(Vaga::empresaId).distinct().count());
    }
    public Vaga buscarPorId(long id) {
        if (id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador inválido.");
        return repository.buscar(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga não encontrada ou encerrada."));
    }
    private void validar(List<String> valores, Set<String> permitidos) {
        if (valores.size() > permitidos.size() || !permitidos.containsAll(valores))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro inválido.");
    }
    private String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}