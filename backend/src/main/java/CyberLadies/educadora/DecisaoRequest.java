package CyberLadies.educadora;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record DecisaoRequest(@NotNull StatusSolicitacao status, @Size(max=1000) String motivo) {
    @JsonAnySetter
    void recusarCampoExtra(String campo, Object valor) {
        throw new IllegalArgumentException("Informe apenas status e motivo da rejeição.");
    }
}
