const base = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')
export async function rascunhos(caminho = '', { method = 'GET', dados, operacao, signal } = {}) {
    const resposta = await fetch(base + '/educadora/materiais/rascunhos' + caminho, {
        method, signal, cache: 'no-store',
        headers: { Authorization: 'Bearer ' + (sessionStorage.getItem('token') || ''), ...(dados ? { 'Content-Type': 'application/json', 'Idempotency-Key': operacao } : {}) },
        body: dados ? JSON.stringify(dados) : undefined,
    })
    if (!resposta.ok) {
        const problema = await resposta.json().catch(() => ({}))
        const erro = new Error(problema.detail || 'Não foi possível salvar ou consultar o rascunho. Tente novamente.')
        erro.status = resposta.status
        throw erro
    }
    return resposta.json()
}
