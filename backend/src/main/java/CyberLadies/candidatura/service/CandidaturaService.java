package CyberLadies.candidatura.service;

import CyberLadies.candidatura.model.Candidatura;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


@Service 
public class CandidaturaService {

    // Armazena temporariamente as candidaturas cadastradas.
    private final List<Candidatura> candidaturas = new ArrayList<>();

    // Gera um ID único para cada nova candidatura.
    private final AtomicLong proximoId = new AtomicLong(1);


    // Cadastra uma nova candidatura na lista.
    public synchronized Candidatura cadastrar(Candidatura candidatura) {

        // Verifica se os dados obrigatórios foram informados.
        if (!dadosValidos(candidatura)) {
            return null;
        }

        // Gera automaticamente um ID para a candidatura.
        candidatura.setId(proximoId.getAndIncrement());

        // Define o status inicial da candidatura.
        candidatura.setStatus("PENDENTE");

        // Armazena a candidatura na lista.
        candidaturas.add(candidatura);

        // Retorna a candidatura cadastrada.
        return candidatura;
    }


    // Retorna todas as candidaturas cadastradas.
    public synchronized List<Candidatura> listarTodos() {

        // Cria uma cópia da lista para proteger os dados originais.
        return new ArrayList<>(candidaturas);
    }


    // Busca uma candidatura pelo seu identificador.
    public synchronized Candidatura buscarPorId(Long id) {

        // Percorre todas as candidaturas cadastradas.
        for (Candidatura candidatura : candidaturas) {

            // Verifica se o ID corresponde ao solicitado.
            if (candidatura.getId().equals(id)) {

                // Retorna a candidatura encontrada.
                return candidatura;
            }
        }

        // Retorna null caso nenhuma candidatura seja encontrada.
        return null;
    }


    // Atualiza os dados de uma candidatura existente.
    public synchronized Candidatura atualizar(
            Long id, Candidatura dadosAtualizados) {

        // Procura a candidatura pelo ID informado.
        Candidatura candidatura = buscarPorId(id);

        // Verifica se a candidatura existe.
        if (candidatura == null) {
            return null;
        }

        // Verifica se os novos dados são válidos.
        if (!dadosValidos(dadosAtualizados)) {
            return null;
        }

        // Atualiza a usuária vinculada à candidatura.
        candidatura.setUsuariaId(dadosAtualizados.getUsuariaId());

        // Atualiza a vaga vinculada à candidatura.
        candidatura.setVagaId(dadosAtualizados.getVagaId());

        // Atualiza o status, caso tenha sido informado.
        if (dadosAtualizados.getStatus() != null
                && !dadosAtualizados.getStatus().isBlank()) {
            candidatura.setStatus(dadosAtualizados.getStatus());
        }

        // Retorna a candidatura com os dados atualizados.
        return candidatura;
    }

    // Exclui uma candidatura pelo seu ID.
    public synchronized boolean excluir(Long id) {

        // Procura a candidatura cadastrada.
        Candidatura candidatura = buscarPorId(id);

        // Verifica se a candidatura existe.
        if (candidatura == null) {
            return false;
        }

        // Remove a candidatura da lista.
        candidaturas.remove(candidatura);

        // Confirma que a exclusão foi realizada.
        return true;
    }

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
