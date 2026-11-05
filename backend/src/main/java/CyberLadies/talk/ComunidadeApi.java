package CyberLadies.talk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
class ComunidadeApi {
    private final JdbcClient db;
    private final AcessoTalk acesso;
    private static final String SELECT_POST = """
        SELECT p.id,u.nome AS autora,p.texto,p.criado_em,
               p.autora_id=:pessoa AS propria,
               EXISTS(SELECT 1 FROM talk_curtida k WHERE k.post_id=p.id AND k.usuaria_id=:pessoa) AS curtido,
               (SELECT count(*) FROM talk_curtida k WHERE k.post_id=p.id) AS curtidas,
               (SELECT count(*) FROM talk_comentario c WHERE c.post_id=p.id) AS comentarios
        FROM talk_post p JOIN talk_usuaria u ON u.id=p.autora_id
        """;

    record Texto(@NotBlank @Size(max=500) String texto) {}
    record Post(long id, String autora, String texto, OffsetDateTime criadoEm, boolean propria,
                boolean curtido, long curtidas, long comentarios) {}
    record Comentario(long id, String autora, String texto) {}

    ComunidadeApi(JdbcClient db, AcessoTalk acesso) {
        this.db = db;
        this.acesso = acesso;
    }

    @GetMapping("/usuaria")
    AcessoTalk.Usuaria usuaria() { return acesso.usuaria(); }

    @PutMapping("/comunidade/adesao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void aderir() {
        db.sql("UPDATE talk_usuaria SET membro=true WHERE id=?").param(acesso.usuaria().id()).update();
    }

    @GetMapping("/posts")
    List<Post> listar() {
        return db.sql(SELECT_POST + " ORDER BY p.criado_em DESC,p.id DESC")
            .param("pessoa", acesso.membro().id()).query(Post.class).list();
    }

    @GetMapping("/posts/{id}")
    Post consultar(@PathVariable long id) { return buscar(id, acesso.membro().id()); }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    Post publicar(@Valid @RequestBody Texto entrada) {
        long autora = acesso.membro().id();
        long id = db.sql("INSERT INTO talk_post(autora_id,texto) VALUES (?,?) RETURNING id")
            .params(autora, entrada.texto().strip()).query(Long.class).single();
        return buscar(id, autora);
    }

    @PutMapping("/posts/{id}")
    @Transactional
    Post editar(@PathVariable long id, @Valid @RequestBody Texto entrada) {
        long autora = acesso.membro().id();
        exigirAutora(id, autora);
        db.sql("UPDATE talk_post SET texto=? WHERE id=?").params(entrada.texto().strip(), id).update();
        return buscar(id, autora);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void excluir(@PathVariable long id) {
        exigirAutora(id, acesso.membro().id());
        db.sql("DELETE FROM talk_post WHERE id=?").param(id).update();
    }

    @PutMapping("/posts/{id}/curtida")
    @Transactional
    Post curtir(@PathVariable long id) {
        long pessoa = acesso.membro().id();
        bloquearPost(id);
        db.sql("INSERT INTO talk_curtida VALUES (?,?) ON CONFLICT DO NOTHING").params(id, pessoa).update();
        return buscar(id, pessoa);
    }

    @DeleteMapping("/posts/{id}/curtida")
    @Transactional
    Post descurtir(@PathVariable long id) {
        long pessoa = acesso.membro().id();
        bloquearPost(id);
        db.sql("DELETE FROM talk_curtida WHERE post_id=? AND usuaria_id=?").params(id, pessoa).update();
        return buscar(id, pessoa);
    }

    @GetMapping("/posts/{id}/comentarios")
    List<Comentario> comentarios(@PathVariable long id) {
        buscar(id, acesso.membro().id());
        return db.sql("""
            SELECT c.id,u.nome AS autora,c.texto FROM talk_comentario c
            JOIN talk_usuaria u ON u.id=c.autora_id WHERE post_id=? ORDER BY c.id
            """).param(id).query(Comentario.class).list();
    }

    @PostMapping("/posts/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    Comentario comentar(@PathVariable long id, @Valid @RequestBody Texto entrada) {
        var autora = acesso.membro();
        bloquearPost(id);
        String texto = entrada.texto().strip();
        long chave = db.sql("INSERT INTO talk_comentario(post_id,autora_id,texto) VALUES (?,?,?) RETURNING id")
            .params(id, autora.id(), texto).query(Long.class).single();
        return new Comentario(chave, autora.nome(), texto);
    }

    private Post buscar(long id, long pessoa) {
        return db.sql(SELECT_POST + " WHERE p.id=:id").param("id", id).param("pessoa", pessoa)
            .query(Post.class).optional().orElseThrow(ComunidadeApi::ausente);
    }

    private long bloquearPost(long id) {
        return db.sql("SELECT autora_id FROM talk_post WHERE id=? FOR UPDATE").param(id)
            .query(Long.class).optional().orElseThrow(ComunidadeApi::ausente);
    }

    private void exigirAutora(long id, long pessoa) {
        if (bloquearPost(id) != pessoa)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas a autora pode alterar a publicação.");
    }

    private static ResponseStatusException ausente() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada.");
    }
}
