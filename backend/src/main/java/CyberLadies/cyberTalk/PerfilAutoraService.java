package CyberLadies.cyberTalk;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
class PerfilAutoraService {
    private final AcessoTalk acesso;
    private final PublicacaoRepository repository;
    PerfilAutoraService(AcessoTalk acesso,PublicacaoRepository repository) { this.acesso=acesso; this.repository=repository; }
    PerfilAutoraResponse consultar(long id) { acesso.membro(); return buscar(id); }
    @Transactional(readOnly=true)
    PublicacoesAutoraResponse publicacoes(long id,int pagina,int tamanho) {
        long pessoa=acesso.membro().id();
        if(pagina<0 || tamanho<1 || tamanho>50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confira a página e o tamanho (1 a 50).");
        buscar(id);
        return new PublicacoesAutoraResponse(repository.porAutora(id,pessoa,pagina,tamanho),pagina,tamanho,repository.totalPorAutora(id));
    }
    private PerfilAutoraResponse buscar(long id) {
        return repository.perfilAutora(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Perfil não encontrado."));
    }
}
