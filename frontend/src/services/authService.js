const API_URL = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')

export async function login(email, senha) {
    let resposta
    try {
        resposta = await fetch(API_URL + '/auth/login', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, senha }),
        })
    } catch (erro) {
        throw new Error('Não foi possível conectar ao servidor. Tente novamente em instantes.', { cause: erro })
    }
    if (resposta.status === 401 || resposta.status === 403) throw new Error('E-mail ou senha incorretos.')
    if (!resposta.ok) throw new Error('Ocorreu um erro ao entrar. Tente novamente.')
    const dados = await resposta.json()
    // Compatibilidade com a API anterior, que só autenticava CyberLadies.
    sessionStorage.setItem('perfil', dados.perfil || 'CYBERLADIE')
    if (dados.statusEducadora) sessionStorage.setItem('statusEducadora', dados.statusEducadora)
    else sessionStorage.removeItem('statusEducadora')
    return dados
}
export async function cadastrar(perfil, dados) {
    let resposta
    try {
        resposta = await fetch(API_URL + '/auth/cadastro/' + perfil, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados),
        })
    } catch (erro) {
        throw new Error('Não foi possível conectar ao servidor. Tente novamente em instantes.', { cause: erro })
    }
    if (resposta.status === 409) {
        const problema = await resposta.json().catch(() => ({}))
        throw new Error(problema.detail || 'E-mail ou CNPJ já cadastrado.')
    }
    if (resposta.status === 400) throw new Error('Confira os dados informados e tente novamente.')
    if (!resposta.ok) throw new Error('Não foi possível criar sua conta. Tente novamente.')
    return resposta.status === 204 ? null : resposta.json().catch(() => null)
}
export async function encerrarSessao() {
    const token = sessionStorage.getItem('token')
    if (token) {
        let resposta
        try {
            resposta = await fetch(API_URL + '/auth/sessao', { method: 'DELETE', headers: { Authorization: 'Bearer ' + token } })
        } catch (erro) {
            throw new Error('Não foi possível encerrar a sessão. Tente novamente.', { cause: erro })
        }
        if (!resposta.ok && resposta.status !== 401) throw new Error('Não foi possível encerrar a sessão. Tente novamente.')
    }
    limparSessao()
    localStorage.removeItem('token')
}
export function limparSessao() {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('perfil')
    sessionStorage.removeItem('statusEducadora')
}
export function perfilAtual() { return sessionStorage.getItem('perfil') || 'CYBERLADIE' }
export function sessaoCyberLadie() { return !!sessionStorage.getItem('token') && perfilAtual() === 'CYBERLADIE' }
export function contaOutroPerfil() { return !!sessionStorage.getItem('token') && perfilAtual() !== 'CYBERLADIE' }
export async function consultarConta(signal) {
    const token = sessionStorage.getItem('token') || ''
    let resposta
    try {
        resposta = await fetch(API_URL + '/auth/conta', {
            headers: { Authorization: 'Bearer ' + token }, signal, cache: 'no-store',
        })
    } catch (erro) {
        if (erro.name === 'AbortError') throw erro
        throw new Error('Não foi possível consultar sua conta. Verifique a conexão e tente novamente.', { cause: erro })
    }
    if (!resposta.ok) {
        if (resposta.status === 401 && !signal?.aborted && sessionStorage.getItem('token') === token) limparSessao()
        const erro = new Error(resposta.status === 401 ? 'Sua sessão expirou. Entre novamente.' : 'Não foi possível consultar sua conta. Tente novamente.')
        erro.status = resposta.status
        throw erro
    }
    const conta = await resposta.json()
    if (!['CYBERLADIE', 'EDUCADORA', 'EMPRESA'].includes(conta.perfil)) throw new Error('Não foi possível confirmar o perfil da conta.')
    return conta
}
export function sessaoEstudante() { return !!sessionStorage.getItem('token') && ['CYBERLADIE', 'EDUCADORA'].includes(perfilAtual()) }
export function contaSemAcessoEstudante() { return !!sessionStorage.getItem('token') && !sessaoEstudante() }