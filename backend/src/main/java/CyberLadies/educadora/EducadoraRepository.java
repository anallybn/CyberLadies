package CyberLadies.educadora;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class EducadoraRepository {
    private final JdbcClient db;

    EducadoraRepository(JdbcClient db) { this.db = db; }

    boolean bloquearConta(long id) {
        return db.sql("SELECT id FROM conta WHERE id=? FOR UPDATE").param(id).query(Long.class).optional().isPresent();
    }

    Optional<SolicitacaoResponse> buscar(long id) {
        return db.sql("SELECT especialidade,instituicao,status,motivo_rejeicao,decidida_em FROM educadora WHERE conta_id=?")
            .param(id).query(SolicitacaoResponse.class).optional();
    }

    SolicitacaoResponse inserir(long id, String especialidade, String instituicao) {
        return db.sql("""
            INSERT INTO educadora(conta_id,especialidade,instituicao,status) VALUES (?,?,?,'PENDENTE')
            RETURNING especialidade,instituicao,status,motivo_rejeicao,decidida_em
            """).params(id,especialidade,instituicao).query(SolicitacaoResponse.class).single();
    }

    void atribuirPapel(long id) {
        db.sql("INSERT INTO conta_papel(conta_id,papel) VALUES (?,'EDUCADORA') ON CONFLICT DO NOTHING")
            .param(id).update();
    }
}
