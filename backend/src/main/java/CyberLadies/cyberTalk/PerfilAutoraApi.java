package CyberLadies.cyberTalk;

import java.util.List;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Projeção da identidade já visível no Talk. Não consulta o perfil privado. */
@RestController
@RequestMapping("/api/comunidade/perfis")
class PerfilAutoraApi {
    record Perfil(long id, String nome) {}
    record Pagina(List<PublicacaoResponse> itens, int pagina, int tamanho, long total) {}
    private final JdbcClient db;
    private final AcessoTalk acesso;
    private final PublicacaoRepository publicacoes;

    PerfilAutoraApi(JdbcClient db, AcessoTalk acesso, PublicacaoRepository publicacoes) {
        this.db=db; this.acesso=acesso; this.publicacoes=publicacoes;
    }

    private Perfil buscar(long id) {
        // Apenas contas que publicam; não funciona como diretório de contas.
        return db.sql("""
            SELECT c.id,c.nome FROM conta c WHERE c.id=?
            AND EXISTS(SELECT 1 FROM talk_post p WHERE p.autora_id=c.id)
            """).param(id).query(Perfil.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Perfil não encontrado."));
    }

    @GetMapping("/{id}")
    ResponseEntity<Perfil> consultar(@PathVariable long id) {
        acesso.membro();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(buscar(id));
    }

    @GetMapping("/{id}/publicacoes")
    @Transactional(readOnly=true)
    ResponseEntity<Pagina> publicacoes(@PathVariable long id,
        @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="10") int tamanho) {
        long pessoa=acesso.membro().id();
        if (pagina<0 || tamanho<1 || tamanho>50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confira a página e o tamanho (1 a 50).");
        buscar(id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
            new Pagina(publicacoes.porAutora(id,pessoa,pagina,tamanho),pagina,tamanho,publicacoes.totalPorAutora(id)));
    }
}
