const base = import.meta.env?.VITE_API_URL || '/api'
let sessao = { token: () => null, expirar: () => {} }
export function configurarSessao(configuracao) { sessao = configuracao }
export class ErroApi extends Error {
    constructor(message, status) { super(message); this.name = 'ErroApi'; this.status = status }
}
export async function api(caminho, { method = 'GET', dados, binario = false, autenticado = true, signal } = {}) {
    const token = autenticado ? sessao.token() : null
    const multipart = dados instanceof FormData
    let resposta
    try {
        resposta = await fetch(base + caminho, { method, signal,
            headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}),
                ...(dados && !multipart ? { 'Content-Type': 'application/json' } : {}) },
            body: multipart ? dados : dados ? JSON.stringify(dados) : undefined,
        })
    } catch (erro) {
        if (erro.name === 'AbortError') throw erro
        throw new ErroApi('Falha de conexão. Tente novamente.', 0)
    }
    if (!resposta.ok) {
        const erro = await resposta.json().catch(() => ({}))
        if (resposta.status === 401 && autenticado) sessao.expirar(token)
        throw new ErroApi(erro.detail || (resposta.status === 403 ? 'Você não tem permissão para esta ação.' :
            resposta.status === 401 ? 'Entre novamente.' : 'Não foi possível concluir a operação.'), resposta.status)
    }
    if (binario) return resposta.blob()
    if (resposta.status === 204) return null
    const texto = await resposta.text()
    if (!texto.trim()) return null
    try { return JSON.parse(texto) }
    catch { throw new ErroApi('Não foi possível ler a resposta do servidor.', resposta.status) }
}
