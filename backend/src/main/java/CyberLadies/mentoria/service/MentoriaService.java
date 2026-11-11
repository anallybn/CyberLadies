package CyberLadies.mentoria.service;

import CyberLadies.mentoria.model.Mentoria;
import org.springframework.stereotype.Service;

@Service 
// Responsável pelas regras de negócio das mentorias.
public class MentoriaService {

    // Verifica se os dados obrigatórios foram preenchidos.
    public boolean dadosValidos(Mentoria mentoria) {

        // Impede a validação de uma mentoria inexistente.
        if (mentoria == null) {
            return false;
        }

        // Confere os dados necessários para a mentoria.
        return mentoria.getEducadoraId() != null
                && mentoria.getUsuariaId() != null
                && mentoria.getTitulo() != null
                && !mentoria.getTitulo().isBlank()
                && mentoria.getDescricao() != null
                && !mentoria.getDescricao().isBlank()
                && mentoria.getDataHorario() != null
                && !mentoria.getDataHorario().isBlank();
    }
}
