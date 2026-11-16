package CyberLadies.auth.repository;

import CyberLadies.auth.dto.ContaDados.*;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ContaRepository {
    private final JdbcClient db;
    public ContaRepository(JdbcClient db) { this.db=db; }
    public long cadastrar(String nome,String email,String hash,Tipo tipo,String competencias) {
        return db.sql("INSERT INTO talk_usuaria(nome,email,senha_hash,competencias,tipo) VALUES (?,?,?,?,?) RETURNING id")
            .params(nome,email,hash,competencias,tipo.name()).query(Long.class).single();
    }
    public void educadora(long id,CadastroEducadora dados) {
        db.sql("INSERT INTO auth_educadora(usuaria_id,especialidade,instituicao) VALUES (?,?,?)")
            .params(id,dados.especialidade().strip(),dados.instituicao().strip()).update();
    }
    public void empresa(long id,CadastroEmpresa dados) {
        long empresa=db.sql("INSERT INTO cyber_empresa(nome) VALUES (?) RETURNING id").param(dados.nome().strip()).query(Long.class).single();
        db.sql("INSERT INTO auth_empresa(usuaria_id,empresa_id,cnpj,area_atuacao) VALUES (?,?,?,?)")
            .params(id,empresa,dados.cnpj(),dados.areaAtuacao().strip()).update();
    }
    public Optional<Credencial> credencial(String email) {
        return db.sql("""
            SELECT u.id,u.senha_hash AS hash,u.tipo,
            CASE u.tipo WHEN 'EDUCADORA' THEN e.usuaria_id IS NOT NULL
            WHEN 'EMPRESA' THEN p.usuaria_id IS NOT NULL ELSE true END AS cadastro_completo,
            e.status AS status_educadora FROM talk_usuaria u
            LEFT JOIN auth_educadora e ON e.usuaria_id=u.id
            LEFT JOIN auth_empresa p ON p.usuaria_id=u.id
            WHERE lower(btrim(u.email))=?
            """).param(email).query(Credencial.class).optional();
    }
    public void criarSessao(long id,String hash) {
        db.sql("DELETE FROM talk_sessao WHERE expira_em<=CURRENT_TIMESTAMP").update();
        db.sql("INSERT INTO talk_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '8 hours')").params(hash,id).update();
    }
    public void encerrarSessao(String hash) { db.sql("DELETE FROM talk_sessao WHERE token_hash=?").param(hash).update(); }
    public Optional<Conta> conta(String hash) {
        return db.sql("""
            SELECT u.id,u.nome,u.email,u.tipo AS perfil,e.status AS status_educadora
            FROM talk_sessao s JOIN talk_usuaria u ON u.id=s.usuaria_id
            LEFT JOIN auth_educadora e ON e.usuaria_id=u.id
            WHERE s.token_hash=? AND s.expira_em>CURRENT_TIMESTAMP
            """).param(hash).query(Conta.class).optional();
    }
}