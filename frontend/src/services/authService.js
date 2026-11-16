import { api } from './api.js'
import { sair, invalidarSessao, obterSessao, atualizarSessao } from './sessaoService.js'
export async function cadastrar(perfil, dados) {
    if (!['usuaria','empresa'].includes(perfil)) throw new Error('Use a conta pessoal e solicite atuação como educadora.')
    return api('/auth/cadastro/' + perfil, { method: 'POST', dados, autenticado: false })
}
export const encerrarSessao = () => sair()
export const limparSessao = (token) => invalidarSessao(token)
export async function consultarConta(signal) {
    await atualizarSessao()
    return api('/auth/conta', { signal })
}
export function perfilAtual() {
    const papeis = obterSessao().conta?.papeis || []
    return papeis.includes('ADMIN') ? 'ADMIN' : papeis.includes('EMPRESA') ? 'EMPRESA' : papeis.includes('EDUCADORA') ? 'EDUCADORA' : 'CYBERLADIE'
}
export const sessaoCyberLadie = () => !!sessionStorage.getItem('token') && perfilAtual() === 'CYBERLADIE'
export const contaOutroPerfil = () => !!sessionStorage.getItem('token') && !sessaoCyberLadie()
export const sessaoEstudante = () => !!sessionStorage.getItem('token') && ['CYBERLADIE','EDUCADORA'].includes(perfilAtual())
export const contaSemAcessoEstudante = () => !!sessionStorage.getItem('token') && !sessaoEstudante()
