import { api, configurarSessao } from './api.js'

let estado = { conta: null, verificada: false, carregando: false, erro: null, expirada: false }
let consulta = null
const ouvintes = new Set()
export const obterSessao = () => estado
export const obterToken = () => sessionStorage.getItem('token')
export function observarSessao(ouvinte) { ouvintes.add(ouvinte); return () => ouvintes.delete(ouvinte) }
function publicar(novo) { estado = { ...estado, ...novo }; ouvintes.forEach(ouvinte => ouvinte()) }
export function temPapel(conta, papel) { return conta?.papeis?.includes(papel) || false }
function guardarPostAtual() {
    const post = new URLSearchParams(window.location.search).get('post')
    if (window.location.pathname === '/comunidade' && /^\d+$/.test(post || '')) sessionStorage.setItem('talk-post', post)
}
function limpar(expirada = false) {
    sessionStorage.removeItem('token'); consulta = null
    publicar({ conta: null, verificada: true, carregando: false, erro: null, expirada })
}
function expirar(token) {
    if (token !== obterToken()) return
    guardarPostAtual(); limpar(true)
}
configurarSessao({ token: obterToken, expirar })
export function atualizarSessao() {
    const token = obterToken()
    if (!token) { limpar(estado.expirada); return Promise.resolve(null) }
    if (consulta?.token === token) return consulta.promessa
    publicar({ carregando: true, erro: null })
    const promessa = api('/auth/me').then(conta => {
        if (token === obterToken()) publicar({ conta, verificada: true, erro: null, expirada: false })
        return conta
    }).catch(erro => {
        if (token === obterToken()) publicar({ erro: erro.message, verificada: false })
        throw erro
    }).finally(() => {
        if (consulta?.promessa === promessa) consulta = null
        if (token === obterToken()) publicar({ carregando: false })
    })
    consulta = { token, promessa }; return promessa
}
export async function entrar(email, senha) {
    const { token } = await api('/auth/login', { method: 'POST', dados: { email, senha }, autenticado: false })
    sessionStorage.setItem('token', token)
    publicar({ conta: null, verificada: false, expirada: false, erro: null })
    return atualizarSessao()
}
export async function sair() {
    guardarPostAtual()
    try { await api('/auth/sessao', { method: 'DELETE' }) }
    catch (erro) { if (erro.status !== 401) throw erro }
    limpar()
}
export function destinoAposLogin(conta, postDaUrl) {
    if (temPapel(conta, 'ADMIN')) return '/admin/educadoras'
    const post = postDaUrl || sessionStorage.getItem('talk-post')
    sessionStorage.removeItem('talk-post')
    return /^\d+$/.test(post || '') ? '/comunidade?post=' + encodeURIComponent(post) : '/comunidade'
}
