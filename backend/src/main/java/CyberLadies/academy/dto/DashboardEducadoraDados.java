package CyberLadies.academy.dto;

import java.time.OffsetDateTime;
import java.util.List;

public final class DashboardEducadoraDados {
    private DashboardEducadoraDados() {}
    public record Indicadores(long materiaisPublicados,long alunasAlcancadas,long conclusoes) {}
    public record Trilha(long id,String titulo,long inscricoes) {}
    public record Mentoria(long id,String aluna,String tema,OffsetDateTime inicio,OffsetDateTime fim) {}
    public record Material(long id,String titulo,String trilha,long aulas,String status) {}
    public record Dashboard(String nome,String statusEducadora,boolean podePublicar,Indicadores indicadores,
        List<Trilha> trilhas,List<Mentoria> mentorias,List<Material> materiais) {}
}
