package CyberLadies.academy.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

public final class AcademyDados {
    private AcademyDados() {}
    public record Trilha(long id,String titulo,String descricao,String categoria,String nivel,int versao,
        boolean elegivelCertificado,long totalAulas,long cargaMinutos) {}
    public record Aula(long id,long trilhaId,String titulo,String descricao,@com.fasterxml.jackson.annotation.JsonIgnore String complementar,int ordem,
        int duracaoMinutos,@com.fasterxml.jackson.annotation.JsonIgnore String videoProvedor,@com.fasterxml.jackson.annotation.JsonIgnore String videoId,String categoria,String nivel) {}
    public record Video(String provedor,String url) {}
    public record AulaDetalhe(Aula aula, Video video, Long proximaAulaId,String conteudoComplementar) {}
    public record Indicadores(long trilhas,long videoaulas,long areas,Integer percentualGratuito) {}
    public record Catalogo(List<Trilha> trilhas,List<Aula> aulas,Indicadores indicadores) {}
    public record Inscricao(long trilhaId,OffsetDateTime criadoEm) {}
    public record Progresso(long trilhaId,long totalAulas,List<Long> aulasConcluidas,int percentual,Long proximaAulaId) {}
    public record Mentora(long id,String nome,String especialidade,String descricao,Integer anosExperiencia,long horariosLivres) {}
    public record Horario(long id,long mentoraId,OffsetDateTime inicio,OffsetDateTime fim) {}
    public record EntradaAgendamento(@NotNull @Positive Long disponibilidadeId) {}
    public record Agendamento(long id,long disponibilidadeId,long mentoraId,String mentora,
        OffsetDateTime inicio,OffsetDateTime fim,String status) {}
    public record Certificado(UUID id,long trilhaId,String nomeUsuaria,String tituloTrilha,int versao,
        long totalAulas,long cargaMinutos,OffsetDateTime emitidoEm) {}
}