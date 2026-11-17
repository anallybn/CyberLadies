package CyberLadies.cyberTalk;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PublicacaoRepository {
    private static final String SELECT_POST = """
        SELECT p.id,p.autora_id,u.nome AS autora,p.texto,p.criado_em,a.nome AS anexo_nome,a.tipo AS anexo_tipo,
               p.autora_id=:pessoa AS propria,
               EXISTS(SELECT 1 FROM talk_curtida k WHERE k.post_id=p.id AND k.usuaria_id=:pessoa) AS curtido,
               (SELECT count(*) FROM talk_curtida k WHERE k.post_id=p.id) AS curtidas,
               (SELECT count(*) FROM talk_comentario c WHERE c.post_id=p.id) AS comentarios
        FROM talk_post p JOIN conta u ON u.id=p.autora_id
        LEFT JOIN talk_anexo a ON a.post_id=p.id
        """;
    private final JdbcClient db;

    PublicacaoRepository(JdbcClient db) { this.db = db; }

    List<PublicacaoResponse> listar(long pessoa) {
        return db.sql(SELECT_POST + " ORDER BY p.criado_em DESC,p.id DESC")
            .param("pessoa", pessoa).query(PublicacaoResponse.class).list();
    }

    Optional<PublicacaoResponse> buscar(long id, long pessoa) {
        return db.sql(SELECT_POST + " WHERE p.id=:id").param("id", id).param("pessoa", pessoa)
            .query(PublicacaoResponse.class).optional();
    }

    // Usa a mesma projeção e a mesma visibilidade do feed: todas as publicações
    // são acessíveis exclusivamente a participantes autenticadas da comunidade.
    List<PublicacaoResponse> porAutora(long autora, long pessoa, int pagina, int tamanho) {
        return db.sql(SELECT_POST + " WHERE p.autora_id=:autora ORDER BY p.criado_em DESC,p.id DESC LIMIT :limite OFFSET :inicio")
            .param("autora", autora).param("pessoa", pessoa).param("limite", tamanho)
            .param("inicio", (long)pagina*tamanho).query(PublicacaoResponse.class).list();
    }

    long totalPorAutora(long autora) {
        return db.sql("SELECT count(*) FROM talk_post WHERE autora_id=?").param(autora).query(Long.class).single();
    }
    Optional<PerfilAutoraResponse> perfilAutora(long id) {
        return db.sql("SELECT c.id,c.nome FROM conta c WHERE c.id=? AND EXISTS(SELECT 1 FROM talk_post p WHERE p.autora_id=c.id)")
            .param(id).query(PerfilAutoraResponse.class).optional();
    }

    Optional<Publicacao> bloquear(long id) {
        return db.sql("SELECT id,autora_id,texto FROM talk_post WHERE id=? FOR UPDATE")
            .param(id).query(Publicacao.class).optional();
    }
    void aderir(long conta) {
        db.sql("INSERT INTO talk_participacao(conta_id) VALUES (?) ON CONFLICT DO NOTHING").param(conta).update();
    }
    long inserir(long autora,String texto) {
        return db.sql("INSERT INTO talk_post(autora_id,texto) VALUES (?,?) RETURNING id").params(autora,texto).query(Long.class).single();
    }
    void editar(long id,String texto) { db.sql("UPDATE talk_post SET texto=? WHERE id=?").params(texto,id).update(); }
    void excluir(long id) { db.sql("DELETE FROM talk_post WHERE id=?").param(id).update(); }
    void salvarAnexo(long id,AnexoPublicacao anexo) {
        db.sql("INSERT INTO talk_anexo(post_id,nome,tipo,dados) VALUES (?,?,?,?)").params(id,anexo.nome(),anexo.tipo(),anexo.dados()).update();
    }
    Optional<AnexoPublicacao> anexo(long id) {
        return db.sql("SELECT nome,tipo,dados FROM talk_anexo WHERE post_id=?").param(id)
            .query((rs,n) -> new AnexoPublicacao(rs.getString("nome"),rs.getString("tipo"),rs.getBytes("dados"))).optional();
    }
    void curtir(long id,long pessoa) { db.sql("INSERT INTO talk_curtida VALUES (?,?) ON CONFLICT DO NOTHING").params(id,pessoa).update(); }
    void descurtir(long id,long pessoa) { db.sql("DELETE FROM talk_curtida WHERE post_id=? AND usuaria_id=?").params(id,pessoa).update(); }
    List<ComentarioResponse> comentarios(long id) {
        return db.sql("SELECT c.id,u.nome AS autora,c.texto FROM talk_comentario c JOIN conta u ON u.id=c.autora_id WHERE post_id=? ORDER BY c.id")
            .param(id).query(ComentarioResponse.class).list();
    }
    long comentar(long id,long autora,String texto) {
        return db.sql("INSERT INTO talk_comentario(post_id,autora_id,texto) VALUES (?,?,?) RETURNING id")
            .params(id,autora,texto).query(Long.class).single();
    }
}
