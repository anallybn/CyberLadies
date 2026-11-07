package CyberLadies.empresa.service;

import CyberLadies.empresa.model.Empresa;

public class EmpresaService {

    // Verifica se a empresa informou os dados obrigatórios.
    public boolean dadosValidos(Empresa empresa) {

        // Impede a validação de uma empresa inexistente.
        if (empresa == null) {
            return false;
        }

        // Confere os campos obrigatórios do cadastro.
        return empresa.getNome() != null
                && !empresa.getNome().isBlank()
                && empresa.getCnpj() != null
                && !empresa.getCnpj().isBlank()
                && empresa.getEmail() != null
                && !empresa.getEmail().isBlank()
                && empresa.getSenhaHash() != null
                && !empresa.getSenhaHash().isBlank();
    }
}
