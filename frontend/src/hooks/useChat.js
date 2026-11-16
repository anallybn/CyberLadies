import { useCallback, useEffect, useRef, useState } from 'react'
import { chat } from '../services/chatService'
const juntar=(anteriores,novas)=>Array.from(new Map([...anteriores,...novas].map(m=>[m.sequencia,m])).values()).sort((a,b)=>a.sequencia-b.sequencia)
export function useChatLista(busca,pagina,atualizacao){
 const [estado,setEstado]=useState({})
 const chave=busca+':'+pagina+':'+atualizacao
 useEffect(()=>{
  const controle=new AbortController();let timer
  async function atualizar(){
   try{
    if(document.visibilityState==='hidden'){timer=setTimeout(atualizar,5000);return}
    const dados=await chat('/conversas?'+new URLSearchParams({busca,pagina}),{signal:controle.signal})
    if(!controle.signal.aborted)setEstado({chave,dados})
   }catch(erro){if(erro.name!=='AbortError')setEstado(ant=>({chave,dados:ant.chave===chave&&![401,403,404].includes(erro.status)?ant.dados:null,erro:erro.message,status:erro.status}))}
   if(!controle.signal.aborted)timer=setTimeout(atualizar,5000)
  }
  timer=setTimeout(atualizar,250)
  return ()=>{controle.abort();clearTimeout(timer)}
 },[busca,pagina,chave])
 return estado.chave===chave?estado:{carregando:true}
}
export function useChatConversa(id){
 const [estado,setEstado]=useState({mensagens:[],carregando:true})
 const [tentativa,setTentativa]=useState(0)
 const mensagens=useRef([]),trava=useRef(false),carregandoAntigas=useRef(false),ativo=useRef(true),envio=useRef(null)
 useEffect(()=>{
  ativo.current=true
  const controle=new AbortController();let timer
  async function atualizar(){
   try{
    if(document.visibilityState==='hidden'){timer=setTimeout(atualizar,5000);return}
    const conversa=await chat('/conversas/'+id,{signal:controle.signal})
    const lista=await chat('/conversas/'+id+'/mensagens'+(mensagens.current.length?'?apos='+mensagens.current.at(-1).sequencia:''),{signal:controle.signal})
    if(controle.signal.aborted)return
    const inicial=!mensagens.current.length
    mensagens.current=juntar(mensagens.current,lista.itens)
    setEstado(ant=>({...ant,conversa,mensagens:mensagens.current,carregando:false,erro:null,status:null,maisAntigas:inicial?lista.mais:ant.maisAntigas}))
   }catch(erro){
    if(erro.name!=='AbortError')setEstado(ant=>({...ant,carregando:false,erro:erro.message,status:erro.status,...([401,403,404].includes(erro.status)?{conversa:null,mensagens:[]}: {})}))
   }
   if(!controle.signal.aborted)timer=setTimeout(atualizar,5000)
  }
  atualizar()
  return ()=>{ativo.current=false;controle.abort();envio.current?.abort();clearTimeout(timer)}
 },[id,tentativa])
 const anteriores=useCallback(async()=>{
  if(carregandoAntigas.current||!mensagens.current.length)return
  carregandoAntigas.current=true;setEstado(ant=>({...ant,buscandoAntigas:true}))
  try{
   const dados=await chat('/conversas/'+id+'/mensagens?antes='+mensagens.current[0].sequencia)
   if(!ativo.current)return
   mensagens.current=juntar(dados.itens,mensagens.current)
   setEstado(ant=>({...ant,mensagens:mensagens.current,maisAntigas:dados.mais,erro:null}))
  }catch(erro){if(ativo.current&&erro.name!=='AbortError')setEstado(ant=>({...ant,erro:erro.message,status:erro.status}))}
  finally{carregandoAntigas.current=false;if(ativo.current)setEstado(ant=>({...ant,buscandoAntigas:false}))}
 },[id])
 const enviar=useCallback(async(texto,chaveEnvio)=>{
  if(trava.current)return false
  trava.current=true;setEstado(ant=>({...ant,enviando:true,erroEnvio:null}))
  envio.current=new AbortController()
  try{
   const mensagem=await chat('/conversas/'+id+'/mensagens',{method:'POST',dados:{texto,chaveEnvio},signal:envio.current.signal})
   if(!ativo.current)return false
   if(mensagem.id)setTentativa(v=>v+1)
   return true
  }catch(erro){
   if(ativo.current&&erro.name!=='AbortError'){setEstado(ant=>({...ant,erroEnvio:erro.message,status:erro.status}));if([403,404,409].includes(erro.status))setTentativa(v=>v+1)}
   return false
  }finally{trava.current=false;if(ativo.current)setEstado(ant=>({...ant,enviando:false}))}
 },[id])
 return {...estado,anteriores,enviar,recarregar:()=>setTentativa(v=>v+1)}
}
