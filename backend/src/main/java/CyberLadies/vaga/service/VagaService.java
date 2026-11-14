package CyberLadies.vaga.service;

import CyberLadies.vaga.model.Vaga;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


@Service 
public class VagaService {

    // Armazena temporariamente as vagas cadastradas.
    private final List<Vaga> vagas = new ArrayList<>();

    // Gera um identificador único para cada vaga.
    private final AtomicLong proximoId = new AtomicLong(1);

    // Cadastra uma nova vaga após validar seus dados.
    public synchronized Vaga cadastrar(Vaga vaga) {

        // Impede o cadastro de uma vaga com dados inválidos.
        if (!dadosValidos(vaga)) {
            throw new IllegalArgumentException("Dados da vaga invalidos.");
        }

        // Gera automaticamente o ID da nova vaga.
        vaga.setId(proximoId.getAndIncrement());

        // Adiciona a vaga à lista temporária.
        vagas.add(vaga);

        // Retorna a vaga cadastrada.
        return vaga;
    }


    // Retorna todas as vagas cadastradas.
    public synchronized List<Vaga> listarTodos() {

        // Cria uma cópia da lista para proteger os dados originais.
        return new ArrayList<>(vagas);
    }


    // Busca uma vaga pelo seu identificador (ID).
    public synchronized Vaga buscarPorId(Long id) {

        // Percorre todas as vagas cadastradas.
        for (Vaga vaga : vagas) {

            // Verifica se o ID da vaga corresponde ao solicitado.
            if (vaga.getId().equals(id)) {

                // Retorna a vaga encontrada.
                return vaga;
            }
        }

        // Retorna null quando nenhuma vaga é encontrada.
        return null;
    }


    // Atualiza os dados de uma vaga existente.
    public synchronized Vaga atualizar(Long id, Vaga dadosAtualizados) {

        // Procura a vaga pelo ID informado.
        Vaga vaga = buscarPorId(id);

        // Verifica se a vaga existe.
        if (vaga == null) {
            throw new IllegalArgumentException("Vaga nao encontrada.");
        }

        // Verifica se os novos dados são válidos.
        if (!dadosValidos(dadosAtualizados)) {
            throw new IllegalArgumentException("Dados da vaga invalidos.");
        }

        // Atualiza os dados, mantendo o ID original.
        vaga.setEmpresaId(dadosAtualizados.getEmpresaId());
        vaga.setTitulo(dadosAtualizados.getTitulo());
        vaga.setDescricao(dadosAtualizados.getDescricao());
        vaga.setRequisitos(dadosAtualizados.getRequisitos());
        vaga.setModalidade(dadosAtualizados.getModalidade());

        // Retorna a vaga com os dados atualizados.
        return vaga;
    }

    // Exclui uma vaga pelo seu identificador (ID).
    public synchronized boolean excluir(Long id) {

        // Procura a vaga que será excluída.
        Vaga vaga = buscarPorId(id);

        // Verifica se a vaga existe.
        if (vaga == null) {
            return false;
        }

        // Remove a vaga da lista.
        vagas.remove(vaga);

        // Confirma que a exclusão foi realizada.
        return true;
    }

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
