package CyberLadies.usuaria;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class UsuariaRepository {
    private final JdbcClient db;

    UsuariaRepository(JdbcClient db) {
        this.db = db;
    }

    void inserir(String nome, String email, String senhaHash, String competencias) {
        // Uma única instrução mantém conta e dados da usuária atômicos.
        db.sql("""
            WITH nova_conta AS (
                INSERT INTO conta(nome,email,senha_hash) VALUES (?,?,?) RETURNING id
            ), papel_inicial AS (
                INSERT INTO conta_papel(conta_id,papel) SELECT id,'USUARIA' FROM nova_conta
            )
            INSERT INTO usuaria(conta_id,competencias) SELECT id,? FROM nova_conta
            """)
            .params(nome, email, senhaHash, competencias).update();
    }

    Optional<ConsultaUsuaria.Dados> buscar(long id) {
        return db.sql("""
            SELECT c.id,c.nome,u.competencias FROM conta c
            JOIN usuaria u ON u.conta_id=c.id WHERE c.id=?
            """).param(id)
            .query(ConsultaUsuaria.Dados.class).optional();
    }
}
