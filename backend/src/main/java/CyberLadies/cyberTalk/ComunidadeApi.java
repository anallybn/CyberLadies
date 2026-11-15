package CyberLadies.cyberTalk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api")
class ComunidadeApi {
    private final JdbcClient db;
    private final AcessoTalk acesso;
    private final PublicacaoConsultaService consultas;

    record Texto(@NotBlank @Size(max=500) String texto) {}
    record Comentario(long id, String autora, String texto) {}

    ComunidadeApi(JdbcClient db, AcessoTalk acesso, PublicacaoConsultaService consultas) {
        this.db = db;
        this.acesso = acesso;
        this.consultas = consultas;
    }

    @GetMapping("/usuaria")
    AcessoTalk.Usuaria usuaria() { return acesso.usuaria(); }

    @PutMapping("/comunidade/adesao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void aderir() {
        db.sql("INSERT INTO talk_participacao(conta_id) VALUES (?) ON CONFLICT DO NOTHING")
            .param(acesso.usuaria().id()).update();
    }

    @GetMapping("/posts")
    List<PublicacaoResponse> listar() {
        return consultas.listar();
    }

    @GetMapping("/posts/{id}")
    PublicacaoResponse consultar(@PathVariable long id) { return consultas.consultar(id); }

    @PostMapping(value="/posts", consumes=MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    PublicacaoResponse publicar(@Valid @RequestBody Texto entrada) {
        long autora = acesso.membro().id();
        return buscar(inserir(autora, entrada.texto().strip()), autora);
    }

    private long inserir(long autora, String texto) {
        return db.sql("INSERT INTO talk_post(autora_id,texto) VALUES (?,?) RETURNING id")
            .params(autora, texto).query(Long.class).single();
    }

    @PostMapping(value="/posts", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    PublicacaoResponse publicarComAnexo(@RequestParam(defaultValue="") String texto,
                          @RequestParam MultipartFile arquivo) throws IOException {
        long autora = acesso.membro().id();
        if (texto.length() > 500 || arquivo.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confira o texto e o arquivo.");
        if (arquivo.getSize() > 5 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O limite é 5 MB.");
        byte[] dados = arquivo.getBytes();
        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank() || nome.length() > 200 || nome.chars().anyMatch(Character::isISOControl)
                || nome.contains("/") || nome.contains("\\"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
        String tipo = tipoDeAnexo(dados);
        long id = inserir(autora, texto.strip());
        db.sql("INSERT INTO talk_anexo(post_id,nome,tipo,dados) VALUES (?,?,?,?)")
            .params(id, nome, tipo, dados).update();
        return buscar(id, autora);
    }

    private static String tipoDeAnexo(byte[] dados) {
        var inicio = java.util.HexFormat.of().formatHex(dados, 0, Math.min(dados.length, 8));
        if (inicio.startsWith("89504e470d0a1a0a")) return "image/png";
        if (inicio.startsWith("ffd8ff")) return "image/jpeg";
        return "application/octet-stream";
    }

    @GetMapping("/posts/{id}/anexo")
    ResponseEntity<byte[]> anexo(@PathVariable long id) {
        acesso.membro();
        return db.sql("SELECT nome,tipo,dados FROM talk_anexo WHERE post_id=?").param(id)
            .query((rs,n) -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rs.getString("tipo")))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename(rs.getString("nome"), StandardCharsets.UTF_8).build().toString())
                .body(rs.getBytes("dados"))).optional().orElseThrow(ComunidadeApi::ausente);
    }

    @PutMapping("/posts/{id}")
    @Transactional
    PublicacaoResponse editar(@PathVariable long id, @Valid @RequestBody Texto entrada) {
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
    PublicacaoResponse curtir(@PathVariable long id) {
        long pessoa = acesso.membro().id();
        bloquearPost(id);
        db.sql("INSERT INTO talk_curtida VALUES (?,?) ON CONFLICT DO NOTHING").params(id, pessoa).update();
        return buscar(id, pessoa);
    }

    @DeleteMapping("/posts/{id}/curtida")
    @Transactional
    PublicacaoResponse descurtir(@PathVariable long id) {
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
            JOIN conta u ON u.id=c.autora_id WHERE post_id=? ORDER BY c.id
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

    private PublicacaoResponse buscar(long id, long pessoa) {
        return consultas.buscar(id, pessoa);
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
