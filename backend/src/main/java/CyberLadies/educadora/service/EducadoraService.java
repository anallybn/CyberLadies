package CyberLadies.educadora.service;

import CyberLadies.educadora.model.Educadora;
import CyberLadies.educadora.model.StatusAprovacao;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


@Service 
// Responsável pelas regras de negócio das educadoras.
public class EducadoraService {

    // Armazena temporariamente as educadoras cadastradas.
    // Futuramente, os dados serão persistidos no PostgreSQL.
    private final List<Educadora> educadoras = new ArrayList<>();

    // Gera um identificador único para cada educadora.
    private final AtomicLong proximoId = new AtomicLong(1);


    // Cadastra uma nova educadora na lista temporária.
    public synchronized Educadora cadastrar(Educadora educadora) {

        // Impede o cadastro de dados inexistentes.
        if (educadora == null) {
            return null;
        }

        // Verifica se os campos obrigatórios foram preenchidos.
        if (educadora.getNome() == null
                || educadora.getNome().isBlank()
                || educadora.getEmail() == null
                || educadora.getEmail().isBlank()
                || educadora.getSenhaHash() == null
                || educadora.getSenhaHash().isBlank()
                || educadora.getEspecialidade() == null
                || educadora.getEspecialidade().isBlank()
                || educadora.getInstituicao() == null
                || educadora.getInstituicao().isBlank()) {
            return null;
        }

        // Define o identificador da nova educadora.
        educadora.setId(proximoId.getAndIncrement());

        // Armazena a educadora temporariamente.
        educadoras.add(educadora);

        // Retorna a educadora cadastrada.
        return educadora;
    }

    // Retorna todas as educadoras cadastradas em memória.
    public synchronized List<Educadora> listarTodas() {

        // Cria uma cópia da lista para evitar alterações externas.
        return new ArrayList<>(educadoras);
    }

    // Busca uma educadora pelo seu identificador.
    public synchronized Educadora buscarPorId(Long id) {

        // Impede a busca com um ID inexistente.
        if (id == null) {
            return null;
        }

        // Percorre a lista de educadoras cadastradas.
        for (Educadora educadora : educadoras) {

            // Verifica se encontrou a educadora desejada.
            if (id.equals(educadora.getId())) {
                return educadora;
            }
        }

        // Retorna null quando nenhuma educadora é encontrada.
        return null;
    }

    // Atualiza os dados profissionais de uma educadora cadastrada.
    public synchronized Educadora atualizar(
            Long id, Educadora dadosAtualizados) {

        // Procura a educadora pelo ID informado.
        Educadora educadora = buscarPorId(id);

        // Verifica se a educadora existe e se recebeu novos dados.
        if (educadora == null || dadosAtualizados == null) {
            return null;
        }

        // Reaproveita a validação e a atualização já existentes.
        try {
            atualizarDados(
                educadora,
                dadosAtualizados.getNome(),
                dadosAtualizados.getEspecialidade(),
                dadosAtualizados.getInstituicao()
            );
        } catch (IllegalArgumentException erro) {
            // Retorna null se algum campo obrigatório for inválido.
            return null;
        }

        // Retorna a educadora com os dados atualizados.
        return educadora;
    }


    // Exclui uma educadora cadastrada em memória.
    public synchronized boolean excluir(Long id) {

        // Procura a educadora pelo ID informado.
        Educadora educadora = buscarPorId(id);

        // Verifica se a educadora existe.
        if (educadora == null) {
            return false;
        }

        // Remove a educadora da lista.
        educadoras.remove(educadora);

        // Confirma que a exclusão foi realizada.
        return true;
    }

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
