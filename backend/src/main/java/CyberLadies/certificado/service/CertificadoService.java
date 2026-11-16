package CyberLadies.certificado.service;

import CyberLadies.certificado.model.Certificado;
import java.time.LocalDate;
import java.util.UUID;

import java.util.ArrayList; // Permite criar uma lista de certificados.
import java.util.List; // Representa uma coleção de certificados.
import java.util.concurrent.atomic.AtomicLong; // Gera IDs sequenciais.

import org.springframework.stereotype.Service;


@Service
public class CertificadoService {

    // Armazena temporariamente os certificados cadastrados.
    private final List<Certificado> certificados = new ArrayList<>();

    // Gera um ID sequencial para cada novo certificado.
    private final AtomicLong proximoId = new AtomicLong(1);

    // Cadastra um certificado na lista em memória.
    public synchronized Certificado cadastrar(
            Long usuariaId, String nomeAtividade) {

        // Usa o método existente para validar os dados,
        // gerar o código UUID e definir a data de emissão.
        Certificado certificado = emitir(usuariaId, nomeAtividade);

        // Define um ID único e sequencial.
        certificado.setId(proximoId.getAndIncrement());

        // Armazena o certificado na lista.
        certificados.add(certificado);

        // Retorna o certificado cadastrado.
        return certificado;
    }

    // Retorna todos os certificados cadastrados.
    public synchronized List<Certificado> listarTodos() {

        // Cria uma cópia da lista para proteger os dados originais.
        return new ArrayList<>(certificados);
    }


    // Busca um certificado pelo seu identificador.
    public synchronized Certificado buscarPorId(Long id) {

        // Evita buscar um ID nulo.
        if (id == null) {
            return null;
        }

        // Percorre os certificados cadastrados.
        for (Certificado certificado : certificados) {

            // Compara o ID informado com o ID do certificado.
            if (id.equals(certificado.getId())) {

                // Retorna o certificado encontrado.
                return certificado;
            }
        }

        // Retorna null se nenhum certificado tiver esse ID.
        return null;
    }


    // Atualiza os dados de um certificado existente.
    public synchronized Certificado atualizar(
            Long id, Certificado dadosAtualizados) {

        // Procura o certificado pelo ID informado.
        Certificado certificado = buscarPorId(id);

        // Verifica se o certificado e os novos dados existem.
        if (certificado == null || dadosAtualizados == null) {
            return null;
        }

        // Verifica se o ID da usuária é válido.
        if (dadosAtualizados.getUsuariaId() == null
                || dadosAtualizados.getUsuariaId() <= 0) {
            return null;
        }

        // Verifica se o nome da atividade foi informado.
        if (dadosAtualizados.getNomeAtividade() == null
                || dadosAtualizados.getNomeAtividade().isBlank()) {
            return null;
        }

        // Atualiza a usuária vinculada ao certificado.
        certificado.setUsuariaId(dadosAtualizados.getUsuariaId());

        // Atualiza o nome da atividade.
        certificado.setNomeAtividade(
                dadosAtualizados.getNomeAtividade().strip());

        // Preserva o ID, a data de emissão e o código de validação.
        return certificado;
    }


    // Exclui um certificado pelo seu identificador.
    public synchronized boolean excluir(Long id) {

        // Procura o certificado pelo ID informado.
        Certificado certificado = buscarPorId(id);

        // Retorna false se o certificado não existir.
        if (certificado == null) {
            return false;
        }

        // Remove o certificado da lista em memória.
        certificados.remove(certificado);

        // Retorna true para indicar que a exclusão funcionou.
        return true;
    }

    // Emite um certificado para uma usuária.
    public Certificado emitir(Long usuariaId, String nomeAtividade) {

        // Verifica se o identificador da usuária é válido.
        if (usuariaId == null || usuariaId <= 0) {
            throw new IllegalArgumentException("Usuária inválida.");
        }

        // Verifica se o nome da atividade foi informado.
        if (nomeAtividade == null || nomeAtividade.isBlank()) {
            throw new IllegalArgumentException("Informe a atividade concluída.");
        }

        // Gera um código único para identificar o certificado.
        String codigo = UUID.randomUUID().toString();

        // Cria o certificado com a data atual.
        return new Certificado(
            null,
            usuariaId,
            nomeAtividade.strip(),
            LocalDate.now(),
            codigo
        );
    }

    // Verifica se o certificado possui um código de validação.
    public boolean validar(Certificado certificado) {

        // Evita erros caso o certificado não exista.
        if (certificado == null) {
            return false;
        }

        // Confere se o código está preenchido e tem formato UUID.
        try {
            UUID.fromString(certificado.getCodigoValidacao());
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }
}

