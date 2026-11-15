package CyberLadies.usuaria;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CadastroRequest(
    @NotBlank @Size(max = 120) String nome,
    @Email @NotBlank @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 128) String senha,
    @Size(max = 500) String competencias
) {}
