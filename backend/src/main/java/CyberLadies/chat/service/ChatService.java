package CyberLadies.chat.service;
import CyberLadies.auth.service.AcessoConta;
import CyberLadies.chat.dto.ChatDados.*;
import CyberLadies.chat.repository.ChatRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ChatService {
 private final AcessoConta acesso;private final ChatRepository db;
 public ChatService(AcessoConta acesso,ChatRepository db){this.acesso=acesso;this.db=db;}
 private long conta(){return acesso.conta().id();}
 private ResponseStatusException erro(HttpStatus s,String t){return new ResponseStatusException(s,t);}
 private Conversa autorizada(UUID id,long conta){return db.conversa(id,conta).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Conversa não encontrada ou indisponível."));}
 private void pagina(int pagina){if(pagina<0||pagina>10000)throw erro(HttpStatus.BAD_REQUEST,"Página inválida.");}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Contextos contextos(int pagina){
  long conta=conta();pagina(pagina);var itens=db.contextos(conta,31,pagina*30);
  return new Contextos(itens.stream().limit(30).toList(),itens.size()>30);
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Conversas listar(String busca,int pagina){
  long conta=conta();pagina(pagina);if(busca.length()>160)throw erro(HttpStatus.BAD_REQUEST,"Pesquisa muito longa.");
  var itens=db.conversas(conta,busca.strip(),31,pagina*30);return new Conversas(itens.stream().limit(30).toList(),itens.size()>30);
 }
 @Transactional
 public Conversa criar(Criacao dados){
  long conta=conta();var x=db.contexto(dados.contexto(),conta).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Vínculo não encontrado."));
  if(!x.aberto()||!x.aprovada())throw erro(HttpStatus.FORBIDDEN,"O vínculo não permite iniciar uma conversa.");
  UUID id=db.criar(x);db.bloquear(id,conta);var conversa=autorizada(id,conta);
  if(!conversa.podeEnviar())throw erro(HttpStatus.FORBIDDEN,"O vínculo não permite iniciar uma conversa.");
  return conversa;
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Conversa detalhe(UUID id){return autorizada(id,conta());}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Historico historico(UUID id,Long antes,Long apos){
  long conta=conta();autorizada(id,conta);
  if(antes!=null&&apos!=null||antes!=null&&antes<=0||apos!=null&&apos<0)throw erro(HttpStatus.BAD_REQUEST,"Cursor inválido.");
  var itens=new ArrayList<>(db.mensagens(id,conta,antes,apos,51));boolean mais=itens.size()>50;
  if(mais)itens.remove(itens.size()-1);if(apos==null)Collections.reverse(itens);
  return new Historico(itens,mais);
 }
 @Transactional
 public Mensagem enviar(UUID id,Envio dados){
  if(dados.texto().isBlank()||dados.texto().codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\t'&&c!='\r'))throw erro(HttpStatus.BAD_REQUEST,"Confira o texto da mensagem.");
  long conta=conta();autorizada(id,conta);db.bloquear(id,conta);var conversa=autorizada(id,conta);
  var repetida=db.repetida(id,conta,dados.chaveEnvio());
  if(repetida.isPresent()){
   if(!repetida.get().texto().equals(dados.texto()))throw erro(HttpStatus.CONFLICT,"Esta tentativa já foi usada para outro conteúdo.");
   return repetida.get();
  }
  if(!conversa.podeEnviar())throw erro(HttpStatus.FORBIDDEN,conversa.motivoBloqueio());
  if(db.recentes(id,conta)>=20)throw erro(HttpStatus.TOO_MANY_REQUESTS,"Aguarde um momento antes de enviar mais mensagens.");
  return db.enviar(id,conta,dados);
 }
 @Transactional
 public void ler(UUID id,Leitura dados){
  long conta=conta();autorizada(id,conta);db.bloquear(id,conta);autorizada(id,conta);
  if(!db.existe(id,dados.sequencia()))throw erro(HttpStatus.BAD_REQUEST,"Mensagem não encontrada nesta conversa.");
  db.ler(id,conta,dados.sequencia());
 }
}
