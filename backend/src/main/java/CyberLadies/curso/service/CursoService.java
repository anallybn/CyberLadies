package CyberLadies.curso.service;

import CyberLadies.curso.model.Curso;
import org.springframework.stereotype.Service;

@Service
public class CursoService {

    // Verifica se os dados obrigatórios do curso são válidos.
    public boolean dadosValidos(Curso curso) {

        // Impede a validação de um curso inexistente.
        if (curso == null) {
            return false;
        }

        // Confere os campos obrigatórios do curso.
        return curso.getEducadoraId() != null
                && curso.getTitulo() != null
                && !curso.getTitulo().isBlank()
                && curso.getDescricao() != null
                && !curso.getDescricao().isBlank()
                && curso.getCategoria() != null
                && !curso.getCategoria().isBlank()
                && curso.getCargaHoraria() > 0;
    }
}
