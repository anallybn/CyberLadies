package CyberLadies.cyberTalk;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PublicacaoRepository {
    private static final String SELECT_POST = """
        SELECT p.id,u.nome AS autora,p.texto,p.criado_em,a.nome AS anexo_nome,a.tipo AS anexo_tipo,
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
}
