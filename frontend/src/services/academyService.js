import { limparSessao } from './authService'
const base = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')
export async function academy(caminho, { method = 'GET', dados, privado = false, signal } = {}) {
    let resposta
    try {
        resposta = await fetch(base + '/academy' + caminho, {
            method, signal, cache: 'no-store',
            headers: {
                ...(privado ? { Authorization: 'Bearer ' + (sessionStorage.getItem('token') || '') } : {}),
                ...(dados ? { 'Content-Type': 'application/json' } : {}),
            },
            body: dados ? JSON.stringify(dados) : undefined,
        })
    } catch (erro) {
        if (erro.name === 'AbortError') throw erro
        throw new Error('Não foi possível conectar ao servidor. Tente novamente.', { cause: erro })
    }
    if (!resposta.ok) {
        const problema = await resposta.json().catch(() => ({}))
        const mensagens = { 400: 'Confira os filtros e os dados.', 401: 'Entre novamente para continuar.',
            403: 'Sua conta não tem permissão para esta operação.', 404: 'Conteúdo não encontrado.',
            409: 'A operação entrou em conflito com os dados atuais. Atualize e tente novamente.' }
        const erro = new Error(mensagens[resposta.status] || 'Não foi possível concluir a operação.')
        if ([400, 403, 404, 409, 422].includes(resposta.status) && problema.detail) erro.message = problema.detail
        erro.status = resposta.status
        if (resposta.status === 401) limparSessao()
        throw erro
    }
    return resposta.status === 204 ? null : resposta.json()
}
export const CATEGORIAS = { FRONTEND: 'Front-end', BACKEND: 'Back-end', DADOS: 'Dados', UX_DESIGN: 'UX/Design', SOFT_SKILLS: 'Soft Skills' }
export const NIVEIS = { INICIANTE: 'Iniciante', INTERMEDIARIO: 'Intermediário', AVANCADO: 'Avançado' }
export const duracao = (minutos) => minutos < 60 ? minutos + ' min' : Math.floor(minutos / 60) + 'h' + (minutos % 60 ? ' ' + minutos % 60 + 'min' : '')
export const horario = (data) => new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo' }).format(new Date(data))