package CyberLadies.academy.dto;

import CyberLadies.auth.dto.ContaDados.Estrita;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class RascunhoDados {
    private RascunhoDados() {}
    public record Entrada(@NotBlank @Size(max=160) String titulo,@Size(max=5000) String descricao,
        @Positive Long trilhaId,@Positive Integer duracaoMinutos,
        @Pattern(regexp="VIDEO") String tipo,
        @Size(max=8) List<@NotBlank @Size(max=30) String> tags,
        @Pattern(regexp="YOUTUBE|VIMEO") String videoProvedor,@Size(max=20) String videoId,
        Boolean usoAutorizado,@Positive Long revisao) implements Estrita {}
    public record Conteudo(String titulo,String descricao,Long trilhaId,Integer duracaoMinutos,
        String tipo,List<String> tags,String videoProvedor,String videoId,boolean usoAutorizado) {}
    public record Rascunho(UUID id,String titulo,String descricao,Long trilhaId,Integer duracaoMinutos,
        String tipo,List<String> tags,String videoProvedor,String videoId,boolean usoAutorizado,
        long revisao,OffsetDateTime criadoEm,OffsetDateTime atualizadoEm) {}
    public record Resumo(UUID id,String titulo,Long trilhaId,String trilha,String categoria,String nivel,
        Integer duracaoMinutos,String tipo,List<String> tags,long revisao,OffsetDateTime criadoEm,OffsetDateTime atualizadoEm) {}
    public record Registro(UUID id,String hashCriacao,UUID ultimaOperacao,String hashUltimaOperacao,long revisao) {}
}
