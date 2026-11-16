const base=(import.meta.env.VITE_API_URL || '/api').replace(/\/$/,'')
export async function consultarAlunas(caminho='',signal) {
    const token=sessionStorage.getItem('token') || ''
    const resposta=await fetch(base+'/educadora/alunas'+caminho,{headers:{Authorization:'Bearer '+token},signal,cache:'no-store'})
    if(!resposta.ok){
        const erro=new Error(resposta.status===403?'O acompanhamento está disponível somente para Educadoras aprovadas.':resposta.status===404?'Inscrição ou trilha não encontrada.':'Não foi possível consultar as alunas. Tente novamente.')
        erro.status=resposta.status;throw erro
    }
    const dados=await resposta.json()
    if(signal?.aborted||sessionStorage.getItem('token')!==token)throw new DOMException('Consulta cancelada.','AbortError')
    return dados
}
