import { useCallback, useEffect, useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { ArrowLeft, MessageCircle, Plus, Search, Sparkles, X } from 'lucide-react'
import TopoLogado from '../components/TopoLogado'
import TopoEducadora from '../components/TopoEducadora'
import ChatConversa from '../components/chat/ChatConversa'
import { useConta } from '../hooks/useConta'
import { useChatLista } from '../hooks/useChat'
import { chat } from '../services/chatService'
import mascoteChat from '../assets/gatinho-chat.png'
import './CyberChat.css'
const perfis={CYBERLADIE:'CyberLadie',EDUCADORA:'Educadora',EMPRESA:'Empresa'}
function NovaConversa({fechar,abrir}){
 const [estado,setEstado]=useState({}),[pagina,setPagina]=useState(0),[tentativa,setTentativa]=useState(0),[criando,setCriando]=useState(false)
 useEffect(()=>{
  const controle=new AbortController()
  chat('/contextos?pagina='+pagina,{signal:controle.signal}).then(dados=>setEstado({dados,pagina,tentativa})).catch(erro=>{if(erro.name!=='AbortError')setEstado({erro:erro.message,status:erro.status,pagina,tentativa})})
  return ()=>controle.abort()
 },[pagina,tentativa])
 if(estado.status===401)return <Navigate to="/login?retorno=%2Fchat" replace />
 const atual=estado.pagina===pagina&&estado.tentativa===tentativa
 async function iniciar(contexto){
  if(criando)return
  setCriando(true)
  try{const conversa=await chat('/conversas',{method:'POST',dados:{contexto}});abrir(conversa.id)}
  catch(erro){setEstado(ant=>({...ant,erro:erro.message,status:erro.status}))}
  finally{setCriando(false)}
 }
 return <section className="cc-contextos" aria-label="Iniciar conversa por vínculo autorizado">
  <div className="cc-contextos-topo"><h2>Nova conversa</h2><button type="button" onClick={fechar} aria-label="Fechar nova conversa"><X size={19}/></button></div>
  <p>Escolha uma inscrição, mentoria confirmada ou candidatura vinculada à sua conta.</p>
  {!atual?<p role="status">Consultando seus vínculos…</p>:<>
   {estado.erro&&<div role="alert" className="cc-alerta">{estado.erro}<button type="button" onClick={()=>setTentativa(v=>v+1)}>Tentar novamente</button></div>}
   {estado.dados&&!estado.dados.itens.length?<p className="cc-estado">Nenhum vínculo disponível nesta página. Trilhas precisam de autoria, mentorias de uma conta vinculada e Educadoras de aprovação.</p>:<ul>{estado.dados?.itens.map(x=><li key={x.chave}><button type="button" disabled={criando} onClick={()=>iniciar(x.chave)}><strong>{x.participante}</strong><span>{perfis[x.perfil]} · {x.titulo}</span></button></li>)}</ul>}
   <div className="cc-paginacao"><button type="button" disabled={!pagina||criando} onClick={()=>setPagina(v=>v-1)}>Anterior</button><span>Página {pagina+1}</span><button type="button" disabled={!estado.dados?.mais||criando} onClick={()=>setPagina(v=>v+1)}>Próxima</button></div>
  </>}
 </section>
}
function ConteudoChat({conta}){
 const [parametros,setParametros]=useSearchParams()
 const id=parametros.get('conversa')
 const [busca,setBusca]=useState(''),[pagina,setPagina]=useState(0),[nova,setNova]=useState(false),[atualizacao,setAtualizacao]=useState(0)
 const atualizar=useCallback(()=>setAtualizacao(v=>v+1),[])
 const lista=useChatLista(busca,pagina,atualizacao)
 const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id||'')
 function abrir(conversa){setParametros({conversa});setNova(false);atualizar()}
 if(lista.status===401)return <Navigate to="/login?retorno=%2Fchat" replace />
 return <>
  {conta.perfil==='EDUCADORA'?<TopoEducadora ativa="chat"/>:<TopoLogado ativa="chat"/>}
  <main className="cc-conteudo">
   <header className="cc-apresentacao"><div><span className="cc-etiqueta">CONEXÕES COM PROPÓSITO</span><h1 className="cc-titulo" aria-label="CyberChat"><span className="cc-titulo-icone" aria-hidden="true"><MessageCircle size={20} strokeWidth={1.8}/></span><span><span className="cc-titulo-cyber">Cyber</span><span className="cc-titulo-chat">Chat</span></span><Sparkles className="cc-titulo-brilho" size={16} strokeWidth={1.5} aria-hidden="true"/></h1><p>Aprendizagem e oportunidades, uma conversa de cada vez.</p></div><button className="cc-nova" type="button" onClick={()=>setNova(v=>!v)}><Plus size={18}/>Nova conversa</button></header>
   {conta.perfil==='EDUCADORA'&&conta.statusEducadora!=='APROVADA'&&<p className="cc-alerta" role="status">O chat com alunas estará disponível após a aprovação da sua conta.</p>}
   {nova&&<NovaConversa fechar={()=>setNova(false)} abrir={abrir}/>}
   <div className="cc-painel-moldura">
    <img className="cc-mascote" src={mascoteChat} alt="" aria-hidden="true" />
   <div className={'cc-painel'+(uuid?' cc-aberto':'')}>
    <aside className="cc-lista" aria-label="Suas conversas">
     <div className="cc-pesquisa"><Search size={18}/><label htmlFor="cc-busca" className="cc-sr">Pesquisar suas conversas</label><input id="cc-busca" type="search" maxLength={160} placeholder="Pesquisar conversas…" value={busca} onChange={e=>{setBusca(e.target.value);setPagina(0)}}/></div>
     {lista.carregando?<p className="cc-estado" role="status">Carregando conversas…</p>:<>
      {lista.erro&&<div className="cc-alerta" role="alert">{lista.erro}<button type="button" onClick={atualizar}>Tentar novamente</button></div>}
      {lista.dados?.itens.length?<ul>{lista.dados.itens.map(c=><li key={c.id}><button className={c.id===id?'cc-item ativo':'cc-item'} type="button" onClick={()=>abrir(c.id)} aria-current={c.id===id?'true':undefined}><span className="cc-avatar" aria-hidden="true">{c.participante.slice(0,1)}</span><span className="cc-item-texto"><strong>{c.participante}</strong><small>{perfis[c.perfil]}</small><span>{c.titulo}</span></span>{c.naoLidas>0&&<span className="cc-nao-lidas" aria-label={c.naoLidas+' mensagens não lidas'}>{c.naoLidas>99?'99+':c.naoLidas}</span>}</button></li>)}</ul>:lista.dados&&<div className="cc-estado"><MessageCircle size={28}/><h2>{busca?'Nenhuma conversa encontrada':'Seu espaço de conversas'}</h2><p>{busca?'Experimente outro nome ou contexto.':'Inicie uma conversa a partir de um vínculo disponível. Suas mensagens aparecerão aqui.'}</p></div>}
      <div className="cc-paginacao"><button type="button" disabled={!pagina} onClick={()=>setPagina(v=>v-1)}>Anterior</button><span>{pagina+1}</span><button type="button" disabled={!lista.dados?.mais} onClick={()=>setPagina(v=>v+1)}>Próxima</button></div>
     </>}
    </aside>
    {uuid?<ChatConversa key={id} id={id} voltar={()=>setParametros({})} atualizarLista={atualizar}/>:<section className="cc-selecione"><MessageCircle size={42}/><h2>{id?'Conversa inválida':'Vamos conversar?'}</h2><p>{id?'Selecione uma conversa da sua lista.':'Escolha uma conversa ou inicie uma nova a partir de seus vínculos.'}</p>{id&&<button type="button" onClick={()=>setParametros({})}><ArrowLeft size={16}/>Voltar à lista</button>}</section>}
   </div>
   </div>
   <p className="cc-privacidade">Conversas privadas entre participantes autorizados. Evite compartilhar dados sensíveis.</p>
  </main>
 </>
}
export default function CyberChat(){
 const {conta,carregando,erro,expirada,recarregar}=useConta()
 if(expirada||!sessionStorage.getItem('token'))return <Navigate to="/login?retorno=%2Fchat" replace />
 return <div className="cc-pagina">{conta?<ConteudoChat key={conta.id+':'+sessionStorage.getItem('token')} conta={conta}/>:<main className="cc-conteudo"><h1 className="cc-titulo" aria-label="CyberChat"><span className="cc-titulo-icone" aria-hidden="true"><MessageCircle size={20} strokeWidth={1.8}/></span><span><span className="cc-titulo-cyber">Cyber</span><span className="cc-titulo-chat">Chat</span></span><Sparkles className="cc-titulo-brilho" size={16} strokeWidth={1.5} aria-hidden="true"/></h1>{carregando?<p role="status">Confirmando sua conta…</p>:<div role="alert"><p>{erro || 'Não foi possível confirmar sua conta.'}</p><button type="button" onClick={recarregar}>Tentar novamente</button></div>}</main>}</div>
}
