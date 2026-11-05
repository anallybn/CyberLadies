const base = import.meta.env.VITE_API_URL || '/api'

export async function talk(caminho, method = 'GET', dados) {
    let resposta
    try {
        resposta = await fetch(base + caminho, {
            method,
            headers: {
                Authorization: `Bearer ${sessionStorage.getItem('token') || ''}`,
                ...(dados ? { 'Content-Type': 'application/json' } : {}),
            },
            body: dados ? JSON.stringify(dados) : undefined,
        })
    } catch {
        throw new Error('Falha de conexão. Tente novamente.')
    }
    if (resposta.status === 401) {
        sessionStorage.removeItem('token')
        window.location.replace('/login?perfil=usuaria')
        throw new Error('Entre novamente.')
    }
    if (!resposta.ok) {
        const erro = await resposta.json().catch(() => ({}))
        throw new Error(erro.detail || 'Não foi possível concluir a operação.')
    }
    return resposta.status === 204 ? null : resposta.json()
}
