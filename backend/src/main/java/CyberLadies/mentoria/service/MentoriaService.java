package CyberLadies.mentoria.service;

import CyberLadies.mentoria.model.Mentoria;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

// Responsável pelas regras de negócio das mentorias.
@Service
public class MentoriaService {


    // Armazena temporariamente as mentorias cadastradas.
    private final List<Mentoria> mentorias = new ArrayList<>();

    // Gera um identificador único para cada mentoria.
    private final AtomicLong proximoId = new AtomicLong(1);

    // Cadastra uma nova mentoria após validar seus dados.
    public synchronized Mentoria cadastrar(Mentoria mentoria) {

        // Impede o cadastro de dados inválidos.
        if (!dadosValidos(mentoria)) {
            throw new IllegalArgumentException("Dados da mentoria invalidos.");
        }

        // Gera e atribui o ID da mentoria.
        mentoria.setId(proximoId.getAndIncrement());

        // Adiciona a mentoria à lista temporária.
        mentorias.add(mentoria);

        // Retorna a mentoria cadastrada.
        return mentoria;
    }

    // Retorna todas as mentorias cadastradas.
    public synchronized List<Mentoria> listarTodos() {

        // Cria uma cópia da lista para proteger os dados originais.
        return new ArrayList<>(mentorias);
    }

    // Busca uma mentoria pelo seu identificador (ID).
    public synchronized Mentoria buscarPorId(Long id) {

        // Percorre todas as mentorias cadastradas.
        for (Mentoria mentoria : mentorias) {

            // Verifica se o ID corresponde ao solicitado.
            if (mentoria.getId().equals(id)) {

                // Retorna a mentoria encontrada.
                return mentoria;
            }
        }

        // Retorna null quando nenhuma mentoria é encontrada.
        return null;
    }


    // Atualiza os dados de uma mentoria existente.
    public synchronized Mentoria atualizar(Long id, Mentoria dadosAtualizados) {

        // Procura a mentoria pelo ID informado.
        Mentoria mentoria = buscarPorId(id);

        // Verifica se a mentoria existe.
        if (mentoria == null) {
            throw new IllegalArgumentException("Mentoria nao encontrada.");
        }

        // Verifica se os novos dados são válidos.
        if (!dadosValidos(dadosAtualizados)) {
            throw new IllegalArgumentException("Dados da mentoria invalidos.");
        }

        // Atualiza os dados, mantendo o ID original.
        mentoria.setEducadoraId(dadosAtualizados.getEducadoraId());
        mentoria.setUsuariaId(dadosAtualizados.getUsuariaId());
        mentoria.setTitulo(dadosAtualizados.getTitulo());
        mentoria.setDescricao(dadosAtualizados.getDescricao());
        mentoria.setDataHorario(dadosAtualizados.getDataHorario());

        // Retorna a mentoria atualizada.
        return mentoria;
    }


    // Exclui uma mentoria pelo seu identificador (ID).
    public synchronized boolean excluir(Long id) {

        // Procura a mentoria que será excluída.
        Mentoria mentoria = buscarPorId(id);

        // Verifica se a mentoria existe.
        if (mentoria == null) {
            return false;
        }

        // Remove a mentoria da lista.
        mentorias.remove(mentoria);

        // Confirma que a exclusão foi realizada.
        return true;
    }

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
