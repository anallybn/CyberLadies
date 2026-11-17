import { API_BASE_URL as base } from './apiUrl.js'
import { limparSessao } from './authService'
export async function chat(caminho,{method='GET',dados,signal}={}){
 const token=sessionStorage.getItem('token') || ''
 let resposta
 try{
  resposta=await fetch(base+'/chat'+caminho,{method,signal,cache:'no-store',headers:{Authorization:'Bearer '+token,...(dados?{'Content-Type':'application/json'}:{})},body:dados?JSON.stringify(dados):undefined})
 }catch(erro){
  if(erro.name==='AbortError')throw erro
  throw new Error('Não foi possível conectar. Seu texto foi preservado; tente novamente.',{cause:erro})
 }
 if(signal?.aborted||sessionStorage.getItem('token')!==token)throw new DOMException('Consulta cancelada.','AbortError')
 if(!resposta.ok){
  const mensagens={400:'Confira o texto e os dados enviados.',401:'Sua sessão expirou. Entre novamente.',403:'O contexto não permite esta ação.',404:'Conversa não encontrada ou indisponível.',409:'Houve um conflito. Atualize a conversa e tente novamente.',429:'Aguarde um momento antes de enviar mais mensagens.'}
  const erro=new Error(mensagens[resposta.status] || 'Não foi possível carregar o CyberChat. Tente novamente.')
  erro.status=resposta.status
  if(resposta.status===401)limparSessao()
  throw erro
 }
 const dadosResposta=resposta.status===204?null:await resposta.json()
 if(signal?.aborted||sessionStorage.getItem('token')!==token)throw new DOMException('Consulta cancelada.','AbortError')
 return dadosResposta
}
