package CyberLadies.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record LoginRequest(@Email @NotBlank @Size(max=254) String email,
                    @NotBlank @Size(max=128) String senha) {}
