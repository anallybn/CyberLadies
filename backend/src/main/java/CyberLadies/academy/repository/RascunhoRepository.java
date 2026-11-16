package CyberLadies.academy.repository;

import CyberLadies.academy.dto.RascunhoDados.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RascunhoRepository {
    private final JdbcClient db;
    public RascunhoRepository(JdbcClient db) { this.db=db; }
    private static final String CAMPOS="id,titulo,descricao,trilha_id,duracao_minutos,tipo,tags,video_provedor,video_id,uso_autorizado,revisao,criado_em,atualizado_em";
    private static List<String> tags(java.sql.ResultSet rs) throws java.sql.SQLException {
        return Arrays.asList((String[])rs.getArray("tags").getArray());
    }
    public Optional<Rascunho> buscar(long dona,UUID id) {
        return db.sql("SELECT "+CAMPOS+" FROM academy_material_rascunho WHERE educadora_id=? AND id=?")
            .params(dona,id).query((rs,n)->new Rascunho(rs.getObject("id",UUID.class),rs.getString("titulo"),rs.getString("descricao"),
                rs.getObject("trilha_id",Long.class),rs.getObject("duracao_minutos",Integer.class),rs.getString("tipo"),tags(rs),
                rs.getString("video_provedor"),rs.getString("video_id"),rs.getBoolean("uso_autorizado"),rs.getLong("revisao"),
                rs.getObject("criado_em",java.time.OffsetDateTime.class),rs.getObject("atualizado_em",java.time.OffsetDateTime.class))).optional();
    }
    public List<Resumo> listar(long dona) {
        return db.sql("""
            SELECT r.id,r.titulo,r.trilha_id,t.titulo trilha,t.categoria,t.nivel,r.duracao_minutos,r.tipo,r.tags,r.revisao,r.criado_em,r.atualizado_em
            FROM academy_material_rascunho r LEFT JOIN academy_trilha t ON t.id=r.trilha_id
            WHERE r.educadora_id=? ORDER BY r.atualizado_em DESC,r.id
            """).param(dona).query((rs,n)->new Resumo(rs.getObject("id",UUID.class),rs.getString("titulo"),rs.getObject("trilha_id",Long.class),
                rs.getString("trilha"),rs.getString("categoria"),rs.getString("nivel"),rs.getObject("duracao_minutos",Integer.class),
                rs.getString("tipo"),tags(rs),rs.getLong("revisao"),rs.getObject("criado_em",java.time.OffsetDateTime.class),
                rs.getObject("atualizado_em",java.time.OffsetDateTime.class))).list();
    }
    public boolean trilhaPropria(long dona,long trilha) {
        return db.sql("SELECT EXISTS(SELECT 1 FROM academy_autoria WHERE trilha_id=? AND educadora_id=?)")
            .params(trilha,dona).query(Boolean.class).single();
    }
    // Array é codificado como JSON pelo serviço sem concatenação em SQL.
    public Optional<UUID> inserir(long dona,Conteudo c,UUID chave,String hash,String tagsJson) {
        return db.sql("""
            INSERT INTO academy_material_rascunho(educadora_id,titulo,descricao,trilha_id,duracao_minutos,tipo,tags,
                video_provedor,video_id,uso_autorizado,chave_criacao,hash_criacao,ultima_operacao,hash_ultima_operacao)
            VALUES (?,?,?,?,?,?,ARRAY(SELECT jsonb_array_elements_text(CAST(? AS jsonb))),?,?,?,?,?,?,?)
            ON CONFLICT(educadora_id,chave_criacao) DO NOTHING RETURNING id
            """).params(dona,c.titulo(),c.descricao()).param(c.trilhaId()).param(c.duracaoMinutos()).param(c.tipo()).param(tagsJson)
            .param(c.videoProvedor()).param(c.videoId()).params(c.usoAutorizado(),chave,hash,chave,hash).query(UUID.class).optional();
    }
    public Registro porChave(long dona,UUID chave) {
        return db.sql("SELECT id,hash_criacao,ultima_operacao,hash_ultima_operacao,revisao FROM academy_material_rascunho WHERE educadora_id=? AND chave_criacao=? FOR UPDATE")
            .params(dona,chave).query(Registro.class).single();
    }
    public Optional<Registro> bloquear(long dona,UUID id) {
        return db.sql("SELECT id,hash_criacao,ultima_operacao,hash_ultima_operacao,revisao FROM academy_material_rascunho WHERE educadora_id=? AND id=? FOR UPDATE")
            .params(dona,id).query(Registro.class).optional();
    }
    public void atualizar(long dona,UUID id,Conteudo c,UUID operacao,String hash,String tagsJson,long revisao) {
        int alteradas=db.sql("""
            UPDATE academy_material_rascunho SET titulo=?,descricao=?,trilha_id=?,duracao_minutos=?,tipo=?,
             tags=ARRAY(SELECT jsonb_array_elements_text(CAST(? AS jsonb))),video_provedor=?,video_id=?,uso_autorizado=?,
             revisao=revisao+1,ultima_operacao=?,hash_ultima_operacao=?
            WHERE educadora_id=? AND id=? AND revisao=?
            """).params(c.titulo(),c.descricao()).param(c.trilhaId()).param(c.duracaoMinutos()).params(c.tipo(),tagsJson)
            .param(c.videoProvedor()).param(c.videoId()).params(c.usoAutorizado(),operacao,hash,dona,id,revisao).update();
        if(alteradas!=1) throw new IllegalStateException("Revisão mudou após bloqueio.");
    }
}
