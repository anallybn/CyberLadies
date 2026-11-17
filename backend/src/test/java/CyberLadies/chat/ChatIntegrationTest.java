package CyberLadies.chat;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import CyberLadies.auth.service.Credenciais;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="DATABASE_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
@org.springframework.test.context.jdbc.Sql(scripts="/db/fixture-conta-integracao.sql",executionPhase=org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS,config=@org.springframework.test.context.jdbc.SqlConfig(separator="@@@"))
@SpringBootTest(properties="spring.datasource.url=jdbc:postgresql://127.0.0.1:55439/cyberladies_auth_chat_test") @AutoConfigureMockMvc @Transactional
class ChatIntegrationTest {
 @Autowired MockMvc mvc;@Autowired JdbcClient db;
 long aluna,educadora,empresa,estranha,trilha,mentoria,candidatura;
 String alunaAuth,educadoraAuth,empresaAuth,estranhaAuth,chaveTrilha,chaveMentoria,chaveCandidatura;
 final String base="/api/chat/conversas";
 long conta(String tipo){
  String hash=Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode("TesteCyberChat123!");
  long id=db.sql("SELECT fixture_conta_integracao(?,?,?,'',?,false)")
   .params("Chat fixture "+tipo,"chat-fixture-"+UUID.randomUUID()+"@example.invalid",hash,tipo).query(Long.class).single();
  if(tipo.equals("EDUCADORA")){
   db.sql("INSERT INTO educadora(conta_id,especialidade,instituicao,status) VALUES (?,'Teste','Teste','PENDENTE')").param(id).update();
   db.sql("UPDATE educadora SET status='APROVADA' WHERE conta_id=?").param(id).update();
  }
  if(tipo.equals("EMPRESA")){
   long e=db.sql("INSERT INTO cyber_empresa(nome) VALUES ('Chat fixture empresa') RETURNING id").query(Long.class).single();
   // Identificação exclusivamente sintética em cópia descartável.
   String cnpj=String.format("%014d",id);
   db.sql("INSERT INTO conta_empresa VALUES (?,?,?,'Teste')").params(id,e,cnpj).update();
  }
  return id;
 }
 String sessao(long id){
  String token=Credenciais.token();
  db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '8 hours')").params(Credenciais.hash(token),id).update();
  return "Bearer "+token;
 }
 @BeforeEach void preparar(){
  assertThat(db.sql("SELECT current_database()").query(String.class).single()).isEqualTo("cyberladies_auth_chat_test");
  aluna=conta("CYBERLADIE");educadora=conta("EDUCADORA");empresa=conta("EMPRESA");estranha=conta("CYBERLADIE");
  alunaAuth=sessao(aluna);educadoraAuth=sessao(educadora);empresaAuth=sessao(empresa);estranhaAuth=sessao(estranha);
  trilha=db.sql("INSERT INTO academy_trilha(titulo,descricao,categoria,nivel) VALUES ('Trilha chat teste','Teste','DADOS','INICIANTE') RETURNING id").query(Long.class).single();
  db.sql("INSERT INTO academy_autoria(trilha_id,educadora_id) VALUES (?,?)").params(trilha,educadora).update();
  // Metadados sintéticos exclusivos de teste; nenhum vídeo é reproduzido.
  db.sql("""
   INSERT INTO academy_aula(trilha_id,titulo,descricao,ordem,duracao_minutos,video_provedor,video_id,uso_autorizado)
   VALUES (?,'Aula chat teste','Teste',1,15,'YOUTUBE','M7lc1UVf-VE',true)
   """).param(trilha).update();
  db.sql("INSERT INTO academy_inscricao VALUES (?,?,CURRENT_TIMESTAMP)").params(aluna,trilha).update();
  long mentora=db.sql("INSERT INTO academy_mentora(usuaria_id,nome,especialidade) VALUES (?,'Chat fixture mentora','Teste') RETURNING id").param(educadora).query(Long.class).single();
  long slot=db.sql("INSERT INTO academy_disponibilidade(mentora_id,inicio,fim,tema) VALUES (?,clock_timestamp()+INTERVAL '10 days',clock_timestamp()+INTERVAL '10 days 1 hour','Mentoria chat teste') RETURNING id").param(mentora).query(Long.class).single();
  mentoria=db.sql("INSERT INTO academy_agendamento(usuaria_id,disponibilidade_id) VALUES (?,?) RETURNING id").params(aluna,slot).query(Long.class).single();
  long vaga=db.sql("INSERT INTO cyber_vaga(empresa_id,titulo,descricao) SELECT empresa_id,'Vaga chat teste','Teste' FROM conta_empresa WHERE conta_id=? RETURNING id").param(empresa).query(Long.class).single();
  candidatura=db.sql("INSERT INTO cyber_candidatura(usuaria_id,vaga_id) VALUES (?,?) RETURNING id").params(aluna,vaga).query(Long.class).single();
  chaveTrilha="TRILHA:"+aluna+":"+trilha;chaveMentoria="MENTORIA:"+mentoria;chaveCandidatura="CANDIDATURA:"+candidatura;
 }
 String criar(String auth,String chave)throws Exception{
  var resultado=mvc.perform(post(base).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"contexto\":\""+chave+"\"}"))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
  return new tools.jackson.databind.ObjectMapper().readTree(resultado.getResponse().getContentAsString()).get("id").asText();
 }
 ResultActions enviar(String auth,String id,String texto,UUID chave)throws Exception{
  return mvc.perform(post(base+"/"+id+"/mensagens").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
   .content("{\"texto\":\""+texto+"\",\"chaveEnvio\":\""+chave+"\"}"));
 }
 @Test void contextosEConversasDosTresPerfisSemDadosPrivados()throws Exception{
  for(String auth:List.of(alunaAuth,educadoraAuth,empresaAuth))mvc.perform(get("/api/chat/contextos").header("Authorization",auth)).andExpect(status().isOk());
  String a=criar(alunaAuth,chaveTrilha),b=criar(educadoraAuth,chaveMentoria),c=criar(empresaAuth,chaveCandidatura);
  assertThat(criar(educadoraAuth,chaveTrilha)).isEqualTo(a);
  mvc.perform(get(base).header("Authorization",alunaAuth)).andExpect(jsonPath("$.itens.length()").value(3));
  mvc.perform(get(base).header("Authorization",empresaAuth)).andExpect(jsonPath("$.itens.length()").value(1));
  String json=mvc.perform(get(base).header("Authorization",alunaAuth)).andReturn().getResponse().getContentAsString();
  assertThat(json).doesNotContain("email","telefone","token","cnpj","senha","competencias");
  enviar(empresaAuth,c,"Sobre sua candidatura",UUID.randomUUID()).andExpect(status().isOk());
  enviar(educadoraAuth,b,"Sobre sua mentoria",UUID.randomUUID()).andExpect(status().isOk());
 }
 @Test void isolamentoEmTodosOsEndpointsEModificacaoDeParticipantes()throws Exception{
  String id=criar(alunaAuth,chaveTrilha);
  mvc.perform(get(base).header("Authorization",estranhaAuth).param("contaId",""+aluna)).andExpect(jsonPath("$.itens").isEmpty());
  for(String caminho:List.of(base+"/"+id,base+"/"+id+"/mensagens"))mvc.perform(get(caminho).header("Authorization",estranhaAuth)).andExpect(status().isNotFound());
  enviar(estranhaAuth,id,"Intrusa",UUID.randomUUID()).andExpect(status().isNotFound());
  mvc.perform(put(base+"/"+id+"/leitura").header("Authorization",estranhaAuth).contentType("application/json").content("{\"sequencia\":1}")).andExpect(status().isNotFound());
  mvc.perform(post(base).header("Authorization",estranhaAuth).contentType("application/json").content("{\"contexto\":\""+chaveTrilha+"\"}")).andExpect(status().isNotFound());
  mvc.perform(post(base).header("Authorization",alunaAuth).contentType("application/json").content("{\"contexto\":\""+chaveTrilha+"\",\"responsavelId\":"+estranha+"}")).andExpect(status().isBadRequest());
  mvc.perform(get(base)).andExpect(status().isUnauthorized());
 }
 @Test void pendenteERevogacaoBloqueiamEducadoraSemApagarHistoricoDaAluna()throws Exception{
  String id=criar(alunaAuth,chaveTrilha);
  enviar(alunaAuth,id,"Mensagem preservada",UUID.randomUUID()).andExpect(status().isOk());
  db.sql("UPDATE educadora SET status='PENDENTE' WHERE conta_id=?").param(educadora).update();
  mvc.perform(get("/api/chat/contextos").header("Authorization",educadoraAuth)).andExpect(jsonPath("$.itens").isEmpty());
  mvc.perform(get(base).header("Authorization",educadoraAuth)).andExpect(jsonPath("$.itens").isEmpty());
  mvc.perform(get(base+"/"+id+"/mensagens").header("Authorization",educadoraAuth)).andExpect(status().isNotFound());
  enviar(educadoraAuth,id,"Sem acesso",UUID.randomUUID()).andExpect(status().isNotFound());
  enviar(alunaAuth,id,"Sem envio",UUID.randomUUID()).andExpect(status().isForbidden());
  mvc.perform(get(base+"/"+id+"/mensagens").header("Authorization",alunaAuth)).andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
 }
 @Test void combinacoesNaoPermitidasENenhumaBuscaGlobal()throws Exception{
  db.sql("INSERT INTO academy_inscricao VALUES (?,?,CURRENT_TIMESTAMP)").params(empresa,trilha).update();
  db.sql("INSERT INTO academy_inscricao VALUES (?,?,CURRENT_TIMESTAMP)").params(educadora,trilha).update();
  for(long pessoa:List.of(empresa,educadora)){
   mvc.perform(post(base).header("Authorization",pessoa==empresa?empresaAuth:educadoraAuth).contentType("application/json")
    .content("{\"contexto\":\"TRILHA:"+pessoa+":"+trilha+"\"}")).andExpect(status().isNotFound());
  }
  mvc.perform(post(base).header("Authorization",alunaAuth).contentType("application/json").content("{\"contexto\":\"CONTA:"+estranha+"\"}")).andExpect(status().isNotFound());
  mvc.perform(post(base).header("Authorization",empresaAuth).contentType("application/json").content("{\"contexto\":\""+chaveMentoria+"\"}")).andExpect(status().isNotFound());
 }
 @Test void encerramentoDeVagaECancelamentoPreservamHistoricoEBloqueiamEnvio()throws Exception{
  String m=criar(alunaAuth,chaveMentoria),c=criar(alunaAuth,chaveCandidatura);
  enviar(alunaAuth,m,"Antes do cancelamento",UUID.randomUUID()).andExpect(status().isOk());
  enviar(alunaAuth,c,"Antes do encerramento",UUID.randomUUID()).andExpect(status().isOk());
  db.sql("UPDATE academy_agendamento SET status='CANCELADO' WHERE id=?").param(mentoria).update();
  db.sql("UPDATE cyber_vaga SET ativa=false WHERE id=(SELECT vaga_id FROM cyber_candidatura WHERE id=?)").param(candidatura).update();
  for(String id:List.of(m,c)){
   mvc.perform(get(base+"/"+id).header("Authorization",alunaAuth)).andExpect(jsonPath("$.podeEnviar").value(false));
   mvc.perform(get(base+"/"+id+"/mensagens").header("Authorization",alunaAuth)).andExpect(jsonPath("$.itens.length()").value(1));
   enviar(alunaAuth,id,"Depois",UUID.randomUUID()).andExpect(status().isForbidden());
  }
 }
 @Test void idempotenciaValidacaoEChavePorAutora()throws Exception{
  String id=criar(alunaAuth,chaveTrilha);UUID chave=UUID.randomUUID();
  var a=enviar(alunaAuth,id,"Mensagem",chave).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  var b=enviar(alunaAuth,id,"Mensagem",chave).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertThat(a).isEqualTo(b);
  enviar(alunaAuth,id,"Outra",chave).andExpect(status().isConflict());
  enviar(educadoraAuth,id,"Resposta",chave).andExpect(status().isOk()).andExpect(jsonPath("$.sequencia").value(2));
  enviar(alunaAuth,id,"   ",UUID.randomUUID()).andExpect(status().isBadRequest());
  enviar(alunaAuth,id,"x".repeat(2001),UUID.randomUUID()).andExpect(status().isBadRequest());
  mvc.perform(get(base+"/"+id+"/mensagens").header("Authorization",alunaAuth).param("antes","1").param("apos","1")).andExpect(status().isBadRequest());
 }
 @Test void paginacaoPesquisaNaoLidasECursorMonotonico()throws Exception{
  String id=criar(alunaAuth,chaveTrilha);UUID uuid=UUID.fromString(id);
  for(int i=1;i<=60;i++)db.sql("INSERT INTO chat_mensagem(conversa_id,autora_id,sequencia,texto,chave_envio) VALUES(?,?,1,?,?)").params(uuid,educadora,"Mensagem "+i,UUID.randomUUID()).update();
  mvc.perform(get(base+"/"+id+"/mensagens").header("Authorization",alunaAuth)).andExpect(jsonPath("$.itens.length()").value(50)).andExpect(jsonPath("$.itens[0].sequencia").value(11)).andExpect(jsonPath("$.mais").value(true));
  mvc.perform(get(base+"/"+id+"/mensagens").header("Authorization",alunaAuth).param("antes","11")).andExpect(jsonPath("$.itens.length()").value(10)).andExpect(jsonPath("$.itens[0].sequencia").value(1));
  mvc.perform(get(base+"/"+id+"/mensagens").header("Authorization",alunaAuth).param("apos","50")).andExpect(jsonPath("$.itens.length()").value(10));
  mvc.perform(get(base+"/"+id).header("Authorization",alunaAuth)).andExpect(jsonPath("$.naoLidas").value(60));
  for(int seq:List.of(45,20))mvc.perform(put(base+"/"+id+"/leitura").header("Authorization",alunaAuth).contentType("application/json").content("{\"sequencia\":"+seq+"}")).andExpect(status().isNoContent());
  mvc.perform(get(base+"/"+id).header("Authorization",alunaAuth)).andExpect(jsonPath("$.naoLidas").value(15));
  mvc.perform(get(base+"/"+id).header("Authorization",educadoraAuth)).andExpect(jsonPath("$.naoLidas").value(0));
  mvc.perform(put(base+"/"+id+"/leitura").header("Authorization",alunaAuth).contentType("application/json").content("{\"sequencia\":999}")).andExpect(status().isBadRequest());
  mvc.perform(get(base).header("Authorization",alunaAuth).param("busca","trILha")).andExpect(jsonPath("$.itens.length()").value(1));
  mvc.perform(get(base).header("Authorization",alunaAuth).param("busca","ninguém")).andExpect(jsonPath("$.itens").isEmpty());
  enviar(educadoraAuth,id,"Limite",UUID.randomUUID()).andExpect(status().isTooManyRequests());
 }
 @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
 void criacaoEEnviosSimultaneos()throws Exception{
  ExecutorService pool=Executors.newFixedThreadPool(4);
  try{
   var criacoes=pool.invokeAll(List.<Callable<String>>of(()->criar(alunaAuth,chaveTrilha),()->criar(educadoraAuth,chaveTrilha)));
   String id=criacoes.get(0).get();assertThat(criacoes.get(1).get()).isEqualTo(id);
   UUID chave=UUID.randomUUID();
   List<Callable<Integer>> tarefas=new ArrayList<>();
   for(int i=0;i<4;i++)tarefas.add(()->enviar(alunaAuth,id,"Retry simultâneo",chave).andReturn().getResponse().getStatus());
   for(var f:pool.invokeAll(tarefas))assertThat(f.get()).isEqualTo(200);
   assertThat(db.sql("SELECT count(*) FROM chat_mensagem WHERE conversa_id=?").param(UUID.fromString(id)).query(Long.class).single()).isEqualTo(1);
   tarefas.clear();
   for(int i=0;i<4;i++)tarefas.add(()->enviar(educadoraAuth,id,"Mensagem concorrente",UUID.randomUUID()).andReturn().getResponse().getStatus());
   for(var f:pool.invokeAll(tarefas))assertThat(f.get()).isEqualTo(200);
   assertThat(db.sql("SELECT array_agg(sequencia ORDER BY sequencia)::text FROM chat_mensagem WHERE conversa_id=?").param(UUID.fromString(id)).query(String.class).single()).isEqualTo("{1,2,3,4,5}");
  }finally{pool.shutdownNow();}
 }

 @Autowired org.springframework.transaction.PlatformTransactionManager transacoes;
 @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
 void revogacaoConcorrenteSerializaEnvio()throws Exception{
  String id=criar(alunaAuth,chaveTrilha);
  ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch travou=new CountDownLatch(1),liberar=new CountDownLatch(1);
  try{
   var revogar=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transacoes).execute(s->{
    db.sql("UPDATE educadora SET status='PENDENTE' WHERE conta_id=?").param(educadora).update();travou.countDown();
    try{if(!liberar.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Tempo de teste esgotado");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
    return true;
   }));
   assertThat(travou.await(5,TimeUnit.SECONDS)).isTrue();
   var envio=pool.submit(()->enviar(alunaAuth,id,"Bloqueada após revogação",UUID.randomUUID()).andReturn().getResponse().getStatus());
   assertThatThrownBy(()->envio.get(250,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
   liberar.countDown();assertThat(revogar.get(10,TimeUnit.SECONDS)).isTrue();assertThat(envio.get(10,TimeUnit.SECONDS)).isEqualTo(403);
   assertThat(db.sql("SELECT count(*) FROM chat_mensagem WHERE conversa_id=?").param(UUID.fromString(id)).query(Long.class).single()).isZero();
  }finally{liberar.countDown();pool.shutdownNow();}
 }
 @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
 void cancelamentoConcorrentePreservaHistoricoEBloqueiaProximoEnvio()throws Exception{
  String id=criar(alunaAuth,chaveMentoria);ExecutorService pool=Executors.newFixedThreadPool(2);
  try{
   var envio=pool.submit(()->enviar(alunaAuth,id,"Concorrente ao cancelamento",UUID.randomUUID()).andReturn().getResponse().getStatus());
   var cancelamento=pool.submit(()->mvc.perform(delete("/api/academy/agendamentos/"+mentoria).header("Authorization",alunaAuth)).andReturn().getResponse().getStatus());
   assertThat(envio.get(10,TimeUnit.SECONDS)).isIn(200,403);assertThat(cancelamento.get(10,TimeUnit.SECONDS)).isEqualTo(204);
   enviar(alunaAuth,id,"Após cancelar",UUID.randomUUID()).andExpect(status().isForbidden());
   mvc.perform(get(base+"/"+id+"/mensagens").header("Authorization",educadoraAuth)).andExpect(status().isOk());
  }finally{pool.shutdownNow();}
 }
 @Test void bancoImpedeParticipanteArbitrarioECursorDeOutraConversa()throws Exception{
  String id=criar(alunaAuth,chaveTrilha),outro=criar(alunaAuth,chaveCandidatura);
  enviar(alunaAuth,outro,"Em outra conversa",UUID.randomUUID()).andExpect(status().isOk());
  db.sql("SAVEPOINT chat_integridade").update();
  assertThatThrownBy(()->db.sql("INSERT INTO chat_participante VALUES (?,?)").params(UUID.fromString(id),estranha).update()).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  db.sql("ROLLBACK TO SAVEPOINT chat_integridade").update();
  mvc.perform(put(base+"/"+id+"/leitura").header("Authorization",alunaAuth).contentType("application/json").content("{\"sequencia\":1}")).andExpect(status().isBadRequest());
  db.sql("SAVEPOINT chat_autoria").update();
  assertThatThrownBy(()->db.sql("UPDATE chat_conversa SET responsavel_id=? WHERE id=?").params(estranha,UUID.fromString(id)).update()).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  db.sql("ROLLBACK TO SAVEPOINT chat_autoria").update();
 }

}
