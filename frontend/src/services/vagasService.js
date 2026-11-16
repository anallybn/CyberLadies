import { limparSessao } from './authService'
const base = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')

export async function requisitar(caminho, { method = 'GET', dados, signal, autenticado = false } = {}) {
    let resposta
    try {
        resposta = await fetch(base + caminho, {
            method, signal,
            headers: {
                ...(dados ? { 'Content-Type': 'application/json' } : {}),
                ...(autenticado ? { Authorization: 'Bearer ' + (sessionStorage.getItem('token') || '') } : {}),
            },
            body: dados ? JSON.stringify(dados) : undefined,
        })
    } catch (erro) {
        if (erro.name === 'AbortError') throw erro
        throw new Error('Não foi possível conectar ao servidor. Tente novamente.', { cause: erro })
    }
    if (!resposta.ok) {
        const problema = await resposta.json().catch(() => ({}))
        const mensagens = {
            400: 'Confira os filtros e os dados informados.',
            401: 'Sua sessão expirou. Entre novamente para se candidatar.',
            403: 'Sua conta não tem permissão para esta operação.',
            404: 'Vaga ou candidatura não encontrada.',
            409: 'Você já se candidatou a esta vaga.',
        }
        const erro = new Error(problema.detail || mensagens[resposta.status] || 'O servidor não conseguiu concluir a operação. Tente novamente.')
        erro.status = resposta.status
        if (resposta.status === 401) limparSessao()
        throw erro
    }
    return resposta.status === 204 ? null : resposta.json()
}

export function listarVagas(filtros, signal) {
    const params = new URLSearchParams()
    if (filtros.busca.trim()) params.set('busca', filtros.busca.trim())
    filtros.nivel.forEach((valor) => params.append('nivel', valor))
    filtros.modalidade.forEach((valor) => params.append('modalidade', valor))
    if (filtros.salarioMinimo) params.set('salarioMinimo', filtros.salarioMinimo)
    for (const chave of ['paraMaes', 'pcd', 'semFormacao']) {
        if (filtros[chave]) params.set(chave, 'true')
    }
    return requisitar('/vagas?' + params, { signal })
}
export const buscarVaga = (id, signal) => requisitar('/vagas/' + encodeURIComponent(id), { signal })
export const minhasCandidaturas = (signal) => requisitar('/candidaturas', { autenticado: true, signal })
export const candidatar = (vagaId) => requisitar('/candidaturas', { method: 'POST', dados: { vagaId }, autenticado: true })