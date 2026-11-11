package CyberLadies.candidatura.service;

import CyberLadies.candidatura.model.Candidatura;
import org.springframework.stereotype.Service;

@Service 
public class CandidaturaService {

    // Verifica se a candidatura possui usuária e vaga.
    public boolean dadosValidos(Candidatura candidatura) {

        // Impede a validação de uma candidatura inexistente.
        if (candidatura == null) {
            return false;
        }

        // Confere se a usuária e a vaga foram informadas.
        return candidatura.getUsuariaId() != null
                && candidatura.getVagaId() != null;
    }
}
