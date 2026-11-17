package CyberLadies.chat.repository;
import CyberLadies.chat.dto.ChatDados.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository
public class ChatRepository {
 private final JdbcClient db;
 public ChatRepository(JdbcClient db){this.db=db;}
 public record Vinculo(String chave,String tipo,long alunaId,long responsavelId,String titulo,Long trilhaId,Long agendamentoId,Long candidaturaId,boolean aberto,boolean aprovada){}
 public Optional<Vinculo> contexto(String chave,long conta){
  return db.sql("SELECT * FROM chat_contexto WHERE chave=? AND ? IN(aluna_id,responsavel_id)").params(chave,conta).query(Vinculo.class).optional();
 }
 public List<Contexto> contextos(long conta,int limite,int offset){
  return db.sql("""
   SELECT x.chave,x.tipo,x.titulo,u.nome participante,CASE WHEN EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=u.id AND cp.papel='EMPRESA') THEN 'EMPRESA' WHEN EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=u.id AND cp.papel='EDUCADORA') THEN 'EDUCADORA' ELSE 'CYBERLADIE' END perfil FROM chat_contexto x
   JOIN conta u ON u.id=CASE WHEN x.aluna_id=:conta THEN x.responsavel_id ELSE x.aluna_id END
   WHERE :conta IN(x.aluna_id,x.responsavel_id) AND x.aberto AND x.aprovada
   ORDER BY x.tipo,x.chave LIMIT :limite OFFSET :offset
   """).param("conta",conta).param("limite",limite).param("offset",offset).query(Contexto.class).list();
 }
 private static final String RESUMO="""
  SELECT c.id,c.tipo,x.titulo,u.nome participante,CASE WHEN EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=u.id AND cp.papel='EMPRESA') THEN 'EMPRESA' WHEN EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=u.id AND cp.papel='EDUCADORA') THEN 'EDUCADORA' ELSE 'CYBERLADIE' END perfil,(x.aberto AND x.aprovada) pode_enviar,
   CASE WHEN NOT x.aprovada THEN 'A Educadora precisa estar aprovada para trocar mensagens.'
    WHEN NOT x.aberto THEN 'Contexto encerrado. O histórico continua disponível.' ELSE '' END motivo_bloqueio,
   (SELECT count(*) FROM chat_mensagem m WHERE m.conversa_id=c.id AND m.autora_id<>:conta
    AND m.sequencia>coalesce(l.ultima_seq,0)) nao_lidas,c.atividade_em
  FROM chat_conversa c JOIN chat_participante p ON p.conversa_id=c.id AND p.conta_id=:conta
  JOIN chat_contexto x ON x.chave=c.contexto_chave AND x.aluna_id=c.aluna_id AND x.responsavel_id=c.responsavel_id
  JOIN conta u ON u.id=CASE WHEN c.aluna_id=:conta THEN c.responsavel_id ELSE c.aluna_id END
  JOIN chat_leitura l ON l.conversa_id=c.id AND l.conta_id=:conta
  WHERE (:conta=c.aluna_id OR x.aprovada)
  """;
 public Optional<Conversa> conversa(UUID id,long conta){
  return db.sql(RESUMO+" AND c.id=:id").param("conta",conta).param("id",id).query(Conversa.class).optional();
 }
 public List<Conversa> conversas(long conta,String busca,int limite,int offset){
  return db.sql(RESUMO+" AND (strpos(lower(u.nome),lower(:busca))>0 OR strpos(lower(x.titulo),lower(:busca))>0) ORDER BY c.atividade_em DESC,c.id LIMIT :limite OFFSET :offset")
   .param("conta",conta).param("busca",busca).param("limite",limite).param("offset",offset).query(Conversa.class).list();
 }
 public UUID criar(Vinculo x){
  return db.sql("""
   INSERT INTO chat_conversa(contexto_chave,tipo,aluna_id,responsavel_id,trilha_id,agendamento_id,candidatura_id)
   VALUES(:chave,:tipo,:aluna,:responsavel,:trilha,:agendamento,:candidatura)
   ON CONFLICT(contexto_chave) DO UPDATE SET contexto_chave=EXCLUDED.contexto_chave RETURNING id
   """).param("chave",x.chave()).param("tipo",x.tipo()).param("aluna",x.alunaId()).param("responsavel",x.responsavelId())
   .param("trilha",x.trilhaId(),java.sql.Types.BIGINT).param("agendamento",x.agendamentoId(),java.sql.Types.BIGINT)
   .param("candidatura",x.candidaturaId(),java.sql.Types.BIGINT).query(UUID.class).single();
 }
 public void bloquear(UUID id,long conta){
  db.sql("SELECT c.id FROM chat_conversa c JOIN chat_participante p ON p.conversa_id=c.id AND p.conta_id=? WHERE c.id=? FOR UPDATE OF c")
   .params(conta,id).query(UUID.class).optional();
  db.sql("SELECT chat_travar_contexto(c) FROM chat_conversa c WHERE c.id=?").param(id).query((rs,n)->true).optional();
 }
 private static final String MENSAGEM="SELECT id,sequencia,texto,autora_id=:conta propria,enviado_em,chave_envio FROM chat_mensagem ";
 public List<Mensagem> mensagens(UUID id,long conta,Long antes,Long apos,int limite){
  String filtro=antes!=null?" AND sequencia<:cursor":apos!=null?" AND sequencia>:cursor":"";
  var q=db.sql(MENSAGEM+"WHERE conversa_id=:id"+filtro+" ORDER BY sequencia "+(apos!=null?"ASC":"DESC")+" LIMIT :limite")
   .param("conta",conta).param("id",id).param("limite",limite);
  if(antes!=null||apos!=null)q.param("cursor",antes!=null?antes:apos);
  return q.query(Mensagem.class).list();
 }
 public Optional<Mensagem> repetida(UUID id,long conta,UUID chave){
  return db.sql(MENSAGEM+"WHERE conversa_id=:id AND autora_id=:conta AND chave_envio=:chave")
   .param("id",id).param("conta",conta).param("chave",chave).query(Mensagem.class).optional();
 }
 public long recentes(UUID id,long conta){
  return db.sql("SELECT count(*) FROM chat_mensagem WHERE conversa_id=? AND autora_id=? AND enviado_em>clock_timestamp()-INTERVAL '1 minute'")
   .params(id,conta).query(Long.class).single();
 }
 public Mensagem enviar(UUID id,long conta,Envio dados){
  db.sql("INSERT INTO chat_mensagem(conversa_id,autora_id,sequencia,texto,chave_envio) VALUES(?,?,1,?,?)")
   .params(id,conta,dados.texto(),dados.chaveEnvio()).update();
  return repetida(id,conta,dados.chaveEnvio()).orElseThrow();
 }
 public boolean existe(UUID id,long sequencia){
  return db.sql("SELECT EXISTS(SELECT 1 FROM chat_mensagem WHERE conversa_id=? AND sequencia=?)").params(id,sequencia).query(Boolean.class).single();
 }
 public void ler(UUID id,long conta,long sequencia){
  db.sql("UPDATE chat_leitura SET ultima_seq=greatest(coalesce(ultima_seq,0),?) WHERE conversa_id=? AND conta_id=?")
   .params(sequencia,id,conta).update();
 }
}
