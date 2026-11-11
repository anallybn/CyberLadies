package CyberLadies.vaga.service;

import CyberLadies.vaga.model.Vaga;
import org.springframework.stereotype.Service;

@Service 
public class VagaService {

    // Verifica se os dados obrigatórios da vaga foram preenchidos.
    public boolean dadosValidos(Vaga vaga) {

        // Impede a validação de uma vaga inexistente.
        if (vaga == null) {
            return false;
        }

        // Confere os dados necessários para uma vaga.
        return vaga.getEmpresaId() != null
                && vaga.getTitulo() != null
                && !vaga.getTitulo().isBlank()
                && vaga.getDescricao() != null
                && !vaga.getDescricao().isBlank()
                && vaga.getRequisitos() != null
                && !vaga.getRequisitos().isBlank()
                && vaga.getModalidade() != null
                && !vaga.getModalidade().isBlank();
    }
}
