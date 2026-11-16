package CyberLadies.academy.dto;
import java.time.OffsetDateTime;
import java.util.List;

public final class AlunasDados {
    private AlunasDados() {}
    public record Indicadores(long totalAlunas,long inscricoesConcluidas,long inscricoesEmAndamento,long inscricoes) {}
    public record Trilha(long id,String titulo) {}
    public record Inscricao(long alunaId,long trilhaId,String nome,String trilha,long totalAulas,long aulasConcluidas,int percentual,OffsetDateTime inscritaEm) {}
    public record Painel(Indicadores indicadores,List<Trilha> trilhas,List<Inscricao> inscricoes) {}
    public record Aula(long id,String titulo,int ordem,boolean concluida,OffsetDateTime concluidaEm) {}
    public record Detalhe(Inscricao inscricao,List<Aula> aulas) {}
}
