package CyberLadies.perfil.dto;

import CyberLadies.auth.dto.ContaDados.Tipo;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class PerfilDados {
    private PerfilDados() {}
    public record Edicao(@NotBlank @Size(max=120) String nome,
        @NotNull @Size(max=2000) String sobre, @Size(max=200) String formacao,
        @Size(max=500) String competencias) {}
    public record CertificacaoEntrada(@NotBlank @Size(max=160) String nome,
        @NotBlank @Size(max=160) String instituicao, @NotNull LocalDate data) {}
    public record ExperienciaEntrada(@NotBlank @Size(max=160) String empresa,
        @NotBlank @Size(max=160) String cargo, @NotNull LocalDate inicio,
        LocalDate fim, @NotNull @Size(max=2000) String descricao) {}
    public record Certificacao(long id, String nome, String instituicao, LocalDate data, String nomePdf) {}
    public record ArquivoPdf(String nome, byte[] dados) {}
    public record Experiencia(long id, String empresa, String cargo, LocalDate inicio,
        LocalDate fim, String descricao) {}
    public record Pessoal(long id, String nome, String email, String competencias,
        String sobre, String formacao, boolean temFoto) {}
    public record Participacao(long publicacoes, long comentarios, long curtidas) {}
    public record AtuacaoEducadora(String especialidade, String instituicao) {}
    public record DadosEmpresa(String cnpj, String areaAtuacao) {}
    public record Perfil(Pessoal pessoal, Tipo tipo, AtuacaoEducadora atuacaoEducadora, DadosEmpresa empresa,
        boolean dadosProfissionais, List<Certificacao> certificacoes, List<Experiencia> experiencias,
        Participacao participacao, Long candidaturas,
        List<CyberLadies.academy.dto.AcademyDados.Certificado> certificadosPlataforma) {}
}