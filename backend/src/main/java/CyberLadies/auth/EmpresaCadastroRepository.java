package CyberLadies.auth;
import CyberLadies.auth.dto.ContaDados.CadastroEmpresa;
import java.util.Locale;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository
class EmpresaCadastroRepository {
    private final JdbcClient db;
    EmpresaCadastroRepository(JdbcClient db) { this.db=db; }
    void inserir(CadastroEmpresa dados,String senhaHash) {
        long id=db.sql("INSERT INTO conta(nome,email,senha_hash) VALUES (?,?,?) RETURNING id")
            .params(dados.nome().strip(),dados.email().strip().toLowerCase(Locale.ROOT),senhaHash).query(Long.class).single();
        db.sql("INSERT INTO conta_papel VALUES (?,'EMPRESA')").param(id).update();
        long empresa=db.sql("INSERT INTO cyber_empresa(nome) VALUES (?) RETURNING id").param(dados.nome().strip()).query(Long.class).single();
        db.sql("INSERT INTO conta_empresa VALUES (?,?,?,?)").params(id,empresa,dados.cnpj(),dados.areaAtuacao().strip()).update();
    }
}
