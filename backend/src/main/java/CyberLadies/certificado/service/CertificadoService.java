package CyberLadies.certificado.service;

import CyberLadies.certificado.model.Certificado;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;


@Service
public class CertificadoService {

    // Emite um certificado para uma usuária.
    public Certificado emitir(Long usuariaId, String nomeAtividade) {

        // Verifica se o identificador da usuária é válido.
        if (usuariaId == null || usuariaId <= 0) {
            throw new IllegalArgumentException("Usuária inválida.");
        }

        // Verifica se o nome da atividade foi informado.
        if (nomeAtividade == null || nomeAtividade.isBlank()) {
            throw new IllegalArgumentException("Informe a atividade concluída.");
        }

        // Gera um código único para identificar o certificado.
        String codigo = UUID.randomUUID().toString();

        // Cria o certificado com a data atual.
        return new Certificado(
            null,
            usuariaId,
            nomeAtividade.strip(),
            LocalDate.now(),
            codigo
        );
    }

    // Verifica se o certificado possui um código de validação.
    public boolean validar(Certificado certificado) {

        // Evita erros caso o certificado não exista.
        if (certificado == null) {
            return false;
        }

        // Confere se o código está preenchido e tem formato UUID.
        try {
            UUID.fromString(certificado.getCodigoValidacao());
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }
}

