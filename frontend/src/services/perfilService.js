import { api } from './api.js'
const perfilApi = (caminho, { imagem = false, ...opcoes } = {}) => api('/perfil' + caminho, { ...opcoes, binario: imagem })
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