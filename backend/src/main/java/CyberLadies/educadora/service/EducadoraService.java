package CyberLadies.educadora.service;

import CyberLadies.educadora.model.Educadora;
import CyberLadies.educadora.model.StatusAprovacao;
import org.springframework.stereotype.Service;

@Service 
// Responsável pelas regras de negócio das educadoras.
public class EducadoraService {

    // Verifica se a educadora pode oferecer cursos e mentorias.
    public boolean podePublicar(Educadora educadora) {

        // Apenas educadoras aprovadas podem publicar.
        return educadora.getStatus() == StatusAprovacao.APROVADA;
    }
    
    // Aprova o cadastro de uma educadora.
    public void aprovar(Educadora educadora) {

        // Impede a aprovação de uma educadora inexistente.
        if (educadora == null) {
            throw new IllegalArgumentException(
                "Educadora não encontrada."
            );
        }

        // Atualiza o status da educadora para APROVADA.
        educadora.aprovar();
    }
    
    // Rejeita o cadastro de uma educadora.
    public void rejeitar(Educadora educadora, String motivo) {

        // Verifica se a educadora existe.
        if (educadora == null) {
            throw new IllegalArgumentException(
                "Educadora não encontrada."
            );
        }

        // Impede a rejeição sem uma justificativa.
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                "O motivo da rejeição é obrigatório."
            );
        }

        // Altera o status para REJEITADA e registra o motivo.
        educadora.rejeitar(motivo);
    }

    // Permite que uma educadora rejeitada solicite nova análise.
    public void solicitarReanalise(Educadora educadora) {

        // Verifica se a educadora existe.
        if (educadora == null) {
            throw new IllegalArgumentException(
                "Educadora não encontrada."
            );
        }

        // Utiliza a regra definida na classe Educadora.
        // Apenas cadastros REJEITADOS podem voltar para PENDENTE.
        educadora.solicitarReanalise();
    }

    // Atualiza as informações pessoais e profissionais da educadora.
    public void atualizarDados(
            Educadora educadora,
            String nome,
            String especialidade,
            String instituicao) {

        // Verifica se a educadora existe.
        if (educadora == null) {
            throw new IllegalArgumentException(
                "Educadora não encontrada."
            );
        }

        // Impede que campos obrigatórios fiquem vazios.
        if (nome == null || nome.isBlank()
                || especialidade == null || especialidade.isBlank()
                || instituicao == null || instituicao.isBlank()) {
            throw new IllegalArgumentException(
                "Todos os campos são obrigatórios."
            );
        }

        // Atualiza os dados utilizando o método da classe Educadora.
        educadora.atualizarDados(
            nome.strip(),
            especialidade.strip(),
            instituicao.strip()
        );
    }


}
