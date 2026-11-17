import { useEffect, useRef, useState } from 'react'
import { ArrowLeft, Send, LockKeyhole } from 'lucide-react'
import { Navigate } from 'react-router-dom'
import { useChatConversa } from '../../hooks/useChat'
import { chat } from '../../services/chatService'
const perfis={CYBERLADIE:'CyberLadie',EDUCADORA:'Educadora',EMPRESA:'Empresa'}
const horario=valor=>new Intl.DateTimeFormat('pt-BR',{dateStyle:'short',timeStyle:'short'}).format(new Date(valor))
export default function ChatConversa({id,voltar,atualizarLista}){
 const dados=useChatConversa(id)
 const [texto,setTexto]=useState(''),[erroLeitura,setErroLeitura]=useState('')
 const chave=useRef(crypto.randomUUID()),historico=useRef(null),fim=useRef(null),acompanhar=useRef(true),ultimaLida=useRef(0),leituraPendente=useRef(false)
 useEffect(()=>{
  if(acompanhar.current&&historico.current)historico.current.scrollTop=historico.current.scrollHeight
 },[dados.mensagens])
 useEffect(()=>{
  if(!dados.conversa||!dados.mensagens.length)return
  const controle=new AbortController()
  const sequencia=dados.mensagens.at(-1).sequencia
  async function ler(){
   if(document.visibilityState==='hidden'||!acompanhar.current||sequencia<=ultimaLida.current||leituraPendente.current)return
   leituraPendente.current=true
   try{
    await chat('/conversas/'+id+'/leitura',{method:'PUT',dados:{sequencia},signal:controle.signal})
    if(!controle.signal.aborted){ultimaLida.current=sequencia;setErroLeitura('');atualizarLista()}
   }catch(erro){if(erro.name!=='AbortError')setErroLeitura(erro.message)}
   finally{leituraPendente.current=false}
  }
  const observar=new IntersectionObserver(entradas=>{if(entradas[0].isIntersecting){acompanhar.current=true;ler()}},{root:historico.current,threshold:1})
  observar.observe(fim.current);document.addEventListener('visibilitychange',ler)
  return ()=>{controle.abort();observar.disconnect();document.removeEventListener('visibilitychange',ler)}
 },[id,dados.mensagens,dados.conversa,atualizarLista])
 if(dados.status===401)return <Navigate to="/login?retorno=%2Fchat" replace />
 async function enviar(e){
  e.preventDefault()
  if(!texto.trim()||dados.enviando||!dados.conversa?.podeEnviar)return
  const enviado=await dados.enviar(texto,chave.current)
  if(enviado){setTexto('');chave.current=crypto.randomUUID();acompanhar.current=true;atualizarLista()}
 }
 return <section className="cc-conversa" aria-label="Conversa selecionada">
  <header className="cc-conversa-topo"><button type="button" className="cc-voltar" onClick={voltar} aria-label="Voltar às conversas"><ArrowLeft size={20}/></button><div><h2>{dados.conversa?.participante || 'Conversa'}</h2>{dados.conversa&&<><span className="cc-perfil">{perfis[dados.conversa.perfil]}</span><p>{dados.conversa.titulo}</p></>}</div></header>
  {dados.carregando?<p className="cc-estado" role="status">Carregando mensagens…</p>:<>
   {dados.erro&&<div className="cc-alerta" role="alert">{dados.erro}<button type="button" onClick={dados.recarregar}>Tentar novamente</button></div>}
   {dados.conversa&&<>
    <div className="cc-historico" ref={historico} onScroll={()=>{const el=historico.current;acompanhar.current=el.scrollHeight-el.scrollTop-el.clientHeight<32}} aria-label="Histórico de mensagens">
     {dados.maisAntigas&&<button className="cc-anteriores" type="button" onClick={()=>{acompanhar.current=false;dados.anteriores()}} disabled={dados.buscandoAntigas}>{dados.buscandoAntigas?'Carregando…':'Carregar mensagens anteriores'}</button>}
     {!dados.mensagens.length&&<div className="cc-estado"><h3>A conversa começa aqui</h3><p>Troque mensagens sobre este vínculo, com respeito e cuidado.</p></div>}
     <ol className="cc-mensagens">{dados.mensagens.map(m=><li key={m.id} className={m.propria?'cc-mensagem propria':'cc-mensagem'}><span className="cc-sr">{m.propria?'Você':dados.conversa.participante}: </span><p>{m.texto}</p><time dateTime={m.enviadoEm}>{horario(m.enviadoEm)}</time></li>)}</ol><div ref={fim} className="cc-fim" aria-hidden="true"/>
    </div>
    {erroLeitura&&<p className="cc-alerta" role="status">Não foi possível atualizar a leitura. Ela será tentada novamente ao atualizar a conversa.</p>}
    {!dados.conversa.podeEnviar&&<p className="cc-bloqueio"><LockKeyhole size={16}/>{dados.conversa.motivoBloqueio}</p>}
    <form className="cc-compositor" onSubmit={enviar}><label htmlFor="cc-texto" className="cc-sr">Escrever mensagem</label><textarea id="cc-texto" rows={2} maxLength={2000} value={texto} onChange={e=>{setTexto(e.target.value);chave.current=crypto.randomUUID()}} placeholder="Escreva sua mensagem…" disabled={!dados.conversa.podeEnviar||dados.enviando}/>
     <div><small>{texto.length}/2000</small><button type="submit" disabled={!dados.conversa.podeEnviar||!texto.trim()||dados.enviando}><Send size={17}/>{dados.enviando?'Enviando…':'Enviar'}</button></div>
     {dados.erroEnvio&&<p role="alert">{dados.erroEnvio} O texto continua neste campo.</p>}
    </form>
   </>}
  </>}
 </section>
}
