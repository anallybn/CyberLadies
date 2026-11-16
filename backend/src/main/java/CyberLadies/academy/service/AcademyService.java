package CyberLadies.academy.service;

import CyberLadies.academy.dto.AcademyDados.*;
import CyberLadies.academy.repository.AcademyRepository;
import CyberLadies.auth.service.AcessoConta;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AcademyService {
    protected final AcademyRepository repository;
    protected final AcessoConta acesso;
    protected final VideoSeguro videos;
    public AcademyService(AcademyRepository repository,AcessoConta acesso,VideoSeguro videos) {
        this.repository=repository; this.acesso=acesso; this.videos=videos;
    }
    public Catalogo catalogo(String busca,String categoria,String nivel) {
        validarFiltros(busca,categoria,nivel);
        var todas=repository.trilhas();
        String termo=normalizar(busca.strip());
        var trilhas=todas.stream().filter(t->(categoria.isEmpty() || categoria.equals(t.categoria())) &&
            (nivel.isEmpty() || nivel.equals(t.nivel())) &&
            normalizar(t.titulo()+" "+t.descricao()).contains(termo)).toList();
        var todasAulas=repository.aulas();
        var aulas=todasAulas.stream().filter(a->(categoria.isEmpty() || categoria.equals(a.categoria())) &&
            (nivel.isEmpty() || nivel.equals(a.nivel())) &&
            normalizar(a.titulo()+" "+a.descricao()+" "+a.complementar()).contains(termo)).toList();
        return new Catalogo(trilhas,aulas,new Indicadores(todas.size(),todasAulas.size(),
            todas.stream().map(Trilha::categoria).distinct().count(),todas.isEmpty()?null:100));
    }
    public Trilha trilha(long id) {
        positivo(id);
        return repository.trilha(id).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Trilha não encontrada."));
    }
    public List<Aula> aulas(long trilha) { trilha(trilha); return repository.aulas(trilha); }
    public AulaDetalhe aula(long id) {
        acesso.estudante().id();
        positivo(id);
        var aula=repository.aula(id).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Aula não encontrada."));
        var lista=repository.aulas(aula.trilhaId());
        Long proxima=lista.stream().filter(a->a.ordem()>aula.ordem()).map(Aula::id).findFirst().orElse(null);
        return new AulaDetalhe(aula,videos.gerar(aula.videoProvedor(),aula.videoId()),proxima,aula.complementar());
    }
    private void validarFiltros(String busca,String categoria,String nivel) {
        if (busca.length()>150 || !categoria.isEmpty() && !Set.of("FRONTEND","BACKEND","DADOS","UX_DESIGN","SOFT_SKILLS").contains(categoria)
            || !nivel.isEmpty() && !Set.of("INICIANTE","INTERMEDIARIO","AVANCADO").contains(nivel))
            throw erro(HttpStatus.BAD_REQUEST,"Filtros inválidos.");
    }
    protected void positivo(long id) { if (id<=0) throw erro(HttpStatus.BAD_REQUEST,"Identificador inválido."); }
    protected ResponseStatusException erro(HttpStatus status,String mensagem) { return new ResponseStatusException(status,mensagem); }
    private String normalizar(String texto) {
        return Normalizer.normalize(texto,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
    }
}