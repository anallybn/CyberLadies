package CyberLadies.empresa.service;

import CyberLadies.empresa.model.Empresa;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


@Service 
public class EmpresaService {

    // Armazena temporariamente as empresas cadastradas.
    private final List<Empresa> empresas = new ArrayList<>();

    // Gera um identificador único para cada empresa.
    private final AtomicLong proximoId = new AtomicLong(1);


    // Cadastra uma nova empresa na lista temporária.
    public synchronized Empresa cadastrar(Empresa empresa) {

        // Verifica se os dados obrigatórios foram informados.
        if (!dadosValidos(empresa)) {
            return null;
        }

        // Gera automaticamente um ID para a empresa.
        empresa.setId(proximoId.getAndIncrement());

        // Adiciona a empresa à lista em memória.
        empresas.add(empresa);

        // Retorna a empresa cadastrada.
        return empresa;
    }

    // Retorna todas as empresas cadastradas, sem expor o hash da senha.
    public synchronized List<Empresa> listarTodos() {

        // Cria uma lista para armazenar os dados públicos.
        List<Empresa> resultado = new ArrayList<>();

        // Percorre todas as empresas cadastradas.
        for (Empresa empresa : empresas) {

            // Cria uma cópia sem o campo senhaHash.
            Empresa empresaPublica = new Empresa();

            empresaPublica.setId(empresa.getId());
            empresaPublica.setNome(empresa.getNome());
            empresaPublica.setCnpj(empresa.getCnpj());
            empresaPublica.setEmail(empresa.getEmail());

            // Adiciona a cópia à lista de resultados.
            resultado.add(empresaPublica);
        }

        // Retorna a lista sem os hashes das senhas.
        return resultado;
    }

    // Busca uma empresa pelo seu identificador.
    public synchronized Empresa buscarPorId(Long id) {

        // Verifica se o ID foi informado.
        if (id == null) {
            return null;
        }

        // Percorre todas as empresas cadastradas.
        for (Empresa empresa : empresas) {

            // Verifica se o ID corresponde ao solicitado.
            if (empresa.getId().equals(id)) {

                // Retorna a empresa encontrada.
                return empresa;
            }
        }

        // Retorna null se a empresa não existir.
        return null;
    }


    // Atualiza os dados de uma empresa existente.
    public synchronized Empresa atualizar(
            Long id, Empresa dadosAtualizados) {

        // Procura a empresa pelo ID informado.
        Empresa empresa = buscarPorId(id);

        // Verifica se a empresa existe.
        if (empresa == null) {
            return null;
        }

        // Verifica se os novos dados foram informados.
        if (dadosAtualizados == null) {
            return null;
        }

        // Valida os campos que podem ser atualizados.
        if (dadosAtualizados.getNome() == null
                || dadosAtualizados.getNome().isBlank()
                || dadosAtualizados.getCnpj() == null
                || dadosAtualizados.getCnpj().isBlank()
                || dadosAtualizados.getEmail() == null
                || dadosAtualizados.getEmail().isBlank()) {
            return null;
        }

        // Atualiza o nome da empresa.
        empresa.setNome(dadosAtualizados.getNome());

        // Atualiza o CNPJ.
        empresa.setCnpj(dadosAtualizados.getCnpj());

        // Atualiza o e-mail.
        empresa.setEmail(dadosAtualizados.getEmail());

        // Mantém o hash da senha original.
        // A alteração de senha terá um processo separado e seguro.

        // Retorna a empresa atualizada.
        return empresa;
    }

    // Exclui uma empresa pelo seu identificador.
    public synchronized boolean excluir(Long id) {

        // Procura a empresa pelo ID informado.
        Empresa empresa = buscarPorId(id);

        // Verifica se a empresa existe.
        if (empresa == null) {
            return false;
        }

        // Remove a empresa da lista em memória.
        empresas.remove(empresa);

        // Confirma que a exclusão foi realizada.
        return true;
    }

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
