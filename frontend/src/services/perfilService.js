import { api } from './api.js'
const perfilApi = (caminho, opcoes) => api('/perfil' + caminho, opcoes)
export const carregarPerfil = (signal) => perfilApi('', { signal })
export const salvarPerfil = (dados) => perfilApi('', { method: 'PUT', dados })
export const carregarFoto = (signal) => perfilApi('/foto', { signal, binario: true })
export const removerFoto = () => perfilApi('/foto', { method: 'DELETE' })
export function enviarFoto(arquivo) {
    const dados = new FormData()
    dados.append('arquivo', arquivo)
    return perfilApi('/foto', { method: 'PUT', dados })
}
export const salvarRegistro = (tipo, id, dados) => perfilApi('/' + tipo + (id ? '/' + id : ''), { method: id ? 'PUT' : 'POST', dados })
export function salvarCertificacao(id, dados, arquivo, removerPdf) {
    if (!arquivo && !removerPdf) return salvarRegistro('certificacoes', id, dados)
    const formulario = new FormData()
    formulario.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }))
    if (arquivo) formulario.append('arquivo', arquivo)
    if (removerPdf) formulario.append('removerPdf', 'true')
    return salvarRegistro('certificacoes', id, formulario)
}
export const carregarCertificacaoPdf = (id, baixar) =>
    perfilApi('/certificacoes/' + id + '/pdf' + (baixar ? '?baixar=true' : ''), { binario: true })
export const excluirRegistro = (tipo, id) => perfilApi('/' + tipo + '/' + id, { method: 'DELETE' })