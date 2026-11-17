package CyberLadies.educadora;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record SolicitacaoRequest(@NotBlank @Size(max=120) String especialidade,
                          @NotBlank @Size(max=200) String instituicao) {
    @JsonAnySetter
    void recusarCampoExtra(String campo, Object valor) {
        throw new IllegalArgumentException("Informe apenas especialidade e instituição.");
    }
}
