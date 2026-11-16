package CyberLadies.auth.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public final class ContaDados {
    private ContaDados() {}
    public enum Tipo { CYBERLADIE, EDUCADORA, EMPRESA }
    // O cliente não pode enviar tipo, status, permissões ou campos fora do contrato.
    public interface Estrita {
        @JsonAnySetter
        default void desconhecido(String campo,Object valor) {
            throw new IllegalArgumentException("Campo não permitido.");
        }
    }
    public record CadastroUsuaria(@NotBlank @Size(max=120) String nome,
        @NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=128) String senha,
        @Size(max=500) String competencias) implements Estrita {}
    public record CadastroEducadora(@NotBlank @Size(max=120) String nome,
        @NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=128) String senha,
        @NotBlank @Size(max=160) String especialidade,@NotBlank @Size(max=160) String instituicao) implements Estrita {}
    public record CadastroEmpresa(@NotBlank @Size(max=120) String nome,
        @NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=128) String senha,
        @NotBlank @Pattern(regexp="[0-9]{14}") String cnpj,
        @NotBlank @Size(max=160) String areaAtuacao) implements Estrita {}
    public record Login(@NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(max=128) String senha) implements Estrita {}
    public record Conta(long id,String nome,String email,Tipo perfil,String statusEducadora) {}
    public record Credencial(long id,String hash,Tipo tipo,boolean cadastroCompleto,String statusEducadora) {}
    public record Sessao(String token,Tipo perfil,String statusEducadora) {}
}