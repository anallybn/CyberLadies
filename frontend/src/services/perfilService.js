import { limparSessao } from './authService'
const base = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')

async function perfilApi(caminho = '', { method = 'GET', dados, signal, imagem = false } = {}) {
    const multipart = dados instanceof FormData
    let resposta
    try {
        resposta = await fetch(base + '/perfil' + caminho, {
            method, signal,
            headers: {
                Authorization: 'Bearer ' + (sessionStorage.getItem('token') || ''),
                ...(dados && !multipart ? { 'Content-Type': 'application/json' } : {}),
            },
            body: multipart ? dados : dados ? JSON.stringify(dados) : undefined,
        })
    } catch (erro) {
        if (erro.name === 'AbortError') throw erro
        throw new Error('Não foi possível conectar ao servidor. Tente novamente.', { cause: erro })
    }
    if (!resposta.ok) {
        const problema = await resposta.json().catch(() => ({}))
        const mensagens = {
            400: 'Confira os campos e as datas informadas.',
            401: 'Sua sessão expirou. Entre novamente.',
            403: 'Você não tem permissão para esta operação.',
            404: 'Registro não encontrado. Atualize o perfil.',
            413: 'A foto deve ter até 2 MB.',
        }
        const erro = new Error(mensagens[resposta.status] || 'Não foi possível concluir a operação. Tente novamente.')
        // Usa detalhes seguros de negócio, sem reproduzir mensagens técnicas do servidor.
        if ([400, 404, 413].includes(resposta.status) && problema.detail && !problema.detail.startsWith('Invalid request'))
            erro.message = problema.detail
        erro.status = resposta.status
        if (resposta.status === 401) limparSessao()
        throw erro
    }
    if (imagem) return resposta.blob()
    return resposta.status === 204 ? null : resposta.json()
}
export const carregarPerfil = (signal) => perfilApi('', { signal })
export const salvarPerfil = (dados) => perfilApi('', { method: 'PUT', dados })
export const carregarFoto = (signal) => perfilApi('/foto', { signal, imagem: true })
export const removerFoto = () => perfilApi('/foto', { method: 'DELETE' })
export function enviarFoto(arquivo) {
    const dados = new FormData()
    dados.append('arquivo', arquivo)
    return perfilApi('/foto', { method: 'PUT', dados })
}
export const salvarRegistro = (tipo, id, dados) => perfilApi('/' + tipo + (id ? '/' + id : ''), { method: id ? 'PUT' : 'POST', dados })
export const excluirRegistro = (tipo, id) => perfilApi('/' + tipo + '/' + id, { method: 'DELETE' })