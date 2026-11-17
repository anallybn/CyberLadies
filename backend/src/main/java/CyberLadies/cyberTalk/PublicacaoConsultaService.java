package CyberLadies.cyberTalk;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class PublicacaoConsultaService {
    private final PublicacaoRepository publicacoes;
    private final AcessoTalk acesso;

    PublicacaoConsultaService(PublicacaoRepository publicacoes, AcessoTalk acesso) {
        this.publicacoes = publicacoes;
        this.acesso = acesso;
    }

    List<PublicacaoResponse> listar() {
        return publicacoes.listar(acesso.membro().id());
    }

    PublicacaoResponse consultar(long id) {
        return buscar(id, acesso.membro().id());
    }

    // Reutilizado pelas operações que já verificaram acesso e precisam ler a resposta após uma alteração.
    PublicacaoResponse buscar(long id, long pessoa) {
        return publicacoes.buscar(id, pessoa)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada."));
    }
}
