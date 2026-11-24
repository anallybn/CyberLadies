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
    public Pessoal pessoal(long conta, boolean dadosProfissionais) {
        return db.sql("""
            SELECT c.id,c.nome,c.email,
            CASE WHEN :profissional THEN p.competencias END AS competencias, c.sobre,
            CASE WHEN :profissional THEN c.formacao END AS formacao,
            EXISTS(SELECT 1 FROM perfil_foto WHERE usuaria_id=c.id) AS tem_foto
            FROM conta c LEFT JOIN usuaria p ON p.conta_id=c.id WHERE c.id=:id
            """).param("profissional",dadosProfissionais).param("id",conta).query(Pessoal.class).single();
    }
    public void editar(long conta, Edicao dados) {
        db.sql("UPDATE conta SET nome=?,sobre=? WHERE id=?")
            .params(dados.nome().strip(),dados.sobre().strip(),conta).update();
    }
    public void editarDadosProfissionais(long conta, Edicao dados) {
        db.sql("UPDATE conta SET formacao=? WHERE id=?").params(dados.formacao().strip(),conta).update();
        db.sql("UPDATE usuaria SET competencias=? WHERE conta_id=?").params(dados.competencias().strip(),conta).update();
    }
    public Participacao participacao(long usuaria) {
        return db.sql("""
            SELECT (SELECT count(*) FROM talk_post WHERE autora_id=:id) AS publicacoes,
            (SELECT count(*) FROM talk_comentario WHERE autora_id=:id) AS comentarios,
            (SELECT count(*) FROM talk_curtida WHERE usuaria_id=:id) AS curtidas
            """).param("id",usuaria).query(Participacao.class).single();
    }
    public long contarCandidaturas(long usuaria) {
        return db.sql("SELECT count(*) FROM cyber_candidatura WHERE usuaria_id=?").param(usuaria).query(Long.class).single();
    }
    public Optional<AtuacaoEducadora> atuacaoEducadora(long conta) {
        return db.sql("SELECT especialidade,instituicao FROM educadora WHERE conta_id=? AND status='APROVADA'")
            .param(conta).query(AtuacaoEducadora.class).optional();
    }
    public Optional<DadosEmpresa> empresa(long conta) {
        return db.sql("SELECT cnpj,area_atuacao FROM conta_empresa WHERE conta_id=?")
            .param(conta).query(DadosEmpresa.class).optional();
    }
    private static final String CONSULTA_CERTIFICACOES = """
        SELECT c.id,c.nome,c.instituicao,c.data,p.nome AS nome_pdf
        FROM perfil_certificacao c LEFT JOIN perfil_certificacao_pdf p ON p.certificacao_id=c.id
        WHERE c.usuaria_id=:titular
        """;

    public List<Certificacao> certificacoes(long titular) {
        return db.sql(CONSULTA_CERTIFICACOES + " ORDER BY c.data DESC,c.id DESC")
            .param("titular",titular).query(Certificacao.class).list();
    }
    public Certificacao certificacao(long titular, long id) {
        return db.sql(CONSULTA_CERTIFICACOES + " AND c.id=:id")
            .param("titular",titular).param("id",id).query(Certificacao.class).single();
    }
    public long adicionarCertificacao(long titular, CertificacaoEntrada dados) {
        return db.sql("INSERT INTO perfil_certificacao(usuaria_id,nome,instituicao,data) VALUES (?,?,?,?) RETURNING id")
            .params(titular,dados.nome().strip(),dados.instituicao().strip(),dados.data()).query(Long.class).single();
    }
    public boolean editarCertificacao(long titular, long id, CertificacaoEntrada dados) {
        return db.sql("UPDATE perfil_certificacao SET nome=?,instituicao=?,data=? WHERE id=? AND usuaria_id=?")
            .params(dados.nome().strip(),dados.instituicao().strip(),dados.data(),id,titular).update()==1;
    }
    public Optional<ArquivoPdf> pdfCertificacao(long titular, long id) {
        return db.sql("""
            SELECT p.nome,p.dados FROM perfil_certificacao_pdf p JOIN perfil_certificacao c ON c.id=p.certificacao_id
            WHERE c.id=? AND c.usuaria_id=?
            """).params(id,titular).query(ArquivoPdf.class).optional();
    }
    // Usados na mesma transação que insere ou atualiza a certificação do titular.
    public void salvarPdfCertificacao(long id, ArquivoPdf pdf) {
        db.sql("""
            INSERT INTO perfil_certificacao_pdf(certificacao_id,nome,dados) VALUES (?,?,?)
            ON CONFLICT (certificacao_id) DO UPDATE SET nome=EXCLUDED.nome,dados=EXCLUDED.dados
            """).params(id,pdf.nome(),pdf.dados()).update();
    }
    public void removerPdfCertificacao(long id) {
        db.sql("DELETE FROM perfil_certificacao_pdf WHERE certificacao_id=?").param(id).update();
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