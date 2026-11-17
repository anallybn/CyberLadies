package CyberLadies.cyberTalk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record TextoPublicacao(@NotBlank @Size(max=500) String texto) {}
