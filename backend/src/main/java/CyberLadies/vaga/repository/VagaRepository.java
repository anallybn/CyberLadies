package CyberLadies.vaga.repository;
import CyberLadies.vaga.model.Vaga;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository
public class VagaRepository {
    private final JdbcClient db;
    private static final String SELECT = """
        SELECT v.id,v.empresa_id,e.nome AS empresa,v.titulo,v.descricao,v.requisitos,
            v.modalidade,v.nivel,v.localizacao,v.salario_minimo,v.salario_maximo,
            v.tecnologias,v.para_maes,v.pcd,v.sem_formacao,v.publicado_em
        FROM cyber_vaga v JOIN cyber_empresa e ON e.id=v.empresa_id WHERE v.ativa=true
        """;
    public VagaRepository(JdbcClient db) { this.db = db; }
    public List<Vaga> listar() {
        return db.sql(SELECT + " ORDER BY v.publicado_em DESC,v.id DESC").query(Vaga.class).list();
    }
    public Optional<Vaga> buscar(long id) {
        return db.sql(SELECT + " AND v.id=?").param(id).query(Vaga.class).optional();
    }
}