package CyberLadies.perfil.repository;

import CyberLadies.perfil.dto.PerfilDados.*;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PerfilRepository {
    private final JdbcClient db;
    public PerfilRepository(JdbcClient db) { this.db=db; }
    public Pessoal pessoal(long usuaria) {
        return db.sql("""
            SELECT u.id,u.nome,u.email,p.competencias,u.sobre,u.formacao,
            EXISTS(SELECT 1 FROM perfil_foto WHERE usuaria_id=u.id) AS tem_foto
            FROM conta u JOIN usuaria p ON p.conta_id=u.id WHERE u.id=?
            """).param(usuaria).query(Pessoal.class).single();
    }
    public void editar(long usuaria, Edicao dados) {
        db.sql("UPDATE conta SET nome=?,sobre=?,formacao=? WHERE id=?")
            .params(dados.nome().strip(),dados.sobre().strip(),dados.formacao().strip(),
                usuaria).update();
        db.sql("UPDATE usuaria SET competencias=? WHERE conta_id=?").params(dados.competencias().strip(),usuaria).update();
    }
    public Participacao participacao(long usuaria) {
        return db.sql("""
            SELECT (SELECT count(*) FROM talk_post WHERE autora_id=:id) AS publicacoes,
            (SELECT count(*) FROM talk_comentario WHERE autora_id=:id) AS comentarios,
            (SELECT count(*) FROM talk_curtida WHERE usuaria_id=:id) AS curtidas,
            (SELECT count(*) FROM cyber_candidatura WHERE usuaria_id=:id) AS candidaturas
            """).param("id",usuaria).query(Participacao.class).single();
    }
    public List<Certificacao> certificacoes(long usuaria) {
        return db.sql("SELECT id,nome,instituicao,data FROM perfil_certificacao WHERE usuaria_id=? ORDER BY data DESC,id DESC")
            .param(usuaria).query(Certificacao.class).list();
    }
    public Certificacao adicionarCertificacao(long usuaria, CertificacaoEntrada dados) {
        return db.sql("INSERT INTO perfil_certificacao(usuaria_id,nome,instituicao,data) VALUES (?,?,?,?) RETURNING id,nome,instituicao,data")
            .params(usuaria,dados.nome().strip(),dados.instituicao().strip(),dados.data()).query(Certificacao.class).single();
    }
    public Optional<Certificacao> editarCertificacao(long usuaria, long id, CertificacaoEntrada dados) {
        return db.sql("UPDATE perfil_certificacao SET nome=?,instituicao=?,data=? WHERE id=? AND usuaria_id=? RETURNING id,nome,instituicao,data")
            .params(dados.nome().strip(),dados.instituicao().strip(),dados.data(),id,usuaria).query(Certificacao.class).optional();
    }
    public boolean excluirCertificacao(long usuaria, long id) {
        return db.sql("DELETE FROM perfil_certificacao WHERE id=? AND usuaria_id=?").params(id,usuaria).update()==1;
    }
    public List<Experiencia> experiencias(long usuaria) {
        return db.sql("SELECT id,empresa,cargo,inicio,fim,descricao FROM perfil_experiencia WHERE usuaria_id=? ORDER BY inicio DESC,id DESC")
            .param(usuaria).query(Experiencia.class).list();
    }
    public Experiencia adicionarExperiencia(long usuaria, ExperienciaEntrada dados) {
        return db.sql("""
            INSERT INTO perfil_experiencia(usuaria_id,empresa,cargo,inicio,fim,descricao)
            VALUES (?,?,?,?,?,?) RETURNING id,empresa,cargo,inicio,fim,descricao
            """).param(usuaria).param(dados.empresa().strip()).param(dados.cargo().strip())
            .param(dados.inicio()).param(dados.fim()).param(dados.descricao().strip()).query(Experiencia.class).single();
    }
    public Optional<Experiencia> editarExperiencia(long usuaria, long id, ExperienciaEntrada dados) {
        return db.sql("""
            UPDATE perfil_experiencia SET empresa=?,cargo=?,inicio=?,fim=?,descricao=?
            WHERE id=? AND usuaria_id=? RETURNING id,empresa,cargo,inicio,fim,descricao
            """).param(dados.empresa().strip()).param(dados.cargo().strip()).param(dados.inicio())
            .param(dados.fim()).param(dados.descricao().strip()).param(id).param(usuaria).query(Experiencia.class).optional();
    }
    public boolean excluirExperiencia(long usuaria,long id) {
        return db.sql("DELETE FROM perfil_experiencia WHERE id=? AND usuaria_id=?").params(id,usuaria).update()==1;
    }
    public Optional<byte[]> foto(long usuaria) {
        return db.sql("SELECT dados FROM perfil_foto WHERE usuaria_id=?").param(usuaria)
            .query((rs,n)->rs.getBytes("dados")).optional();
    }
    public void salvarFoto(long usuaria,byte[] dados) {
        db.sql("INSERT INTO perfil_foto VALUES (?,?) ON CONFLICT (usuaria_id) DO UPDATE SET dados=EXCLUDED.dados")
            .params(usuaria,dados).update();
    }
    public void excluirFoto(long usuaria) {
        db.sql("DELETE FROM perfil_foto WHERE usuaria_id=?").param(usuaria).update();
    }
}