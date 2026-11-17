import { api } from './api.js'
export async function consultarSolicitacao() {
    try { return await api('/educadora/solicitacao') }
    catch (erro) { if (erro.status === 404) return null; throw erro }
}
export function solicitarEducadora(especialidade, instituicao) {
    return api('/educadora/solicitacao', { method: 'POST', dados: { especialidade, instituicao } })
}
export function listarSolicitacoes({ status = '', pagina = 0, tamanho = 20 }, signal) {
    const params = new URLSearchParams({ pagina, tamanho })
    if (status) params.set('status', status)
    return api('/admin/educadoras/solicitacoes?' + params.toString(), { signal })
}
export function consultarAnalise(id, signal) { return api('/admin/educadoras/solicitacoes/' + encodeURIComponent(id), { signal }) }
export function decidirSolicitacao(id, status, motivo) {
    return api('/admin/educadoras/solicitacoes/' + encodeURIComponent(id) + '/decisao', {
        method: 'POST', dados: status === 'REJEITADA' ? { status, motivo } : { status },
    })
}
