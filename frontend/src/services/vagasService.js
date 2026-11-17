import { api } from './api.js'
const requisitar = (caminho, { autenticado = false, ...opcoes } = {}) => api(caminho, { ...opcoes, autenticado })
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