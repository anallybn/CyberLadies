import { API_BASE_URL as base } from './apiUrl.js'
import { rascunhos } from './rascunhosService'

async function consultar(caminho, token, signal, privada = false) {
    const resposta = await fetch(base + caminho, {
        signal, cache: 'no-store',
        headers: privada ? { Authorization: 'Bearer ' + token } : {},
    })
    if (!resposta.ok) {
        const erro = new Error('Não foi possível carregar seus materiais. Tente novamente.')
        erro.status = resposta.status
        throw erro
    }
    return resposta.json()
}

export async function consultarMeusMateriais(signal) {
    const token = sessionStorage.getItem('token') || ''
    if (!token) { const erro = new Error('Entre novamente para continuar.'); erro.status = 401; throw erro }
    // Autoria é autorizada pelo servidor, pelo token; não recebemos ID de Educadora.
    const dashboard = await consultar('/educadora/dashboard', token, signal, true)
    const trilhas = dashboard.trilhas
    if (!Array.isArray(trilhas)) throw new Error('Não foi possível confirmar suas trilhas.')
    const grupos = new Array(trilhas.length)
    let proxima = 0
    async function carregar() {
        while (proxima < trilhas.length) {
            const indice = proxima++
            const trilha = trilhas[indice]
            if (!Number.isSafeInteger(trilha.id) || trilha.id <= 0) throw new Error('A consulta de trilhas retornou dados inválidos.')
            // Somente metadados públicos de aulas. Nunca consulta detalhes privados ou rascunhos.
            const aulas = await consultar('/academy/trilhas/' + trilha.id + '/aulas', token, signal)
            if (!Array.isArray(aulas) || aulas.some((aula) => aula.trilhaId !== trilha.id))
                throw new Error('Não foi possível confirmar os materiais desta trilha.')
            grupos[indice] = aulas.map((aula) => ({
                id: aula.id, titulo: aula.titulo, trilhaId: trilha.id, trilha: trilha.titulo,
                categoria: aula.categoria, nivel: aula.nivel, duracaoMinutos: aula.duracaoMinutos,
                ordem: aula.ordem, status: 'PUBLICADO',
            }))
        }
    }
    await Promise.all(Array.from({ length: Math.min(4, trilhas.length) }, carregar))
    if (signal?.aborted || sessionStorage.getItem('token') !== token)
        throw new DOMException('Consulta cancelada.', 'AbortError')
    const publicados = grupos.flat()
    // Não mostra uma lista incompleta como se estivesse completa.
    if (publicados.length !== dashboard.indicadores.materiaisPublicados)
        throw new Error('Seus materiais foram atualizados durante a consulta. Tente novamente.')
    const privados = await rascunhos('', { signal })
    if (signal?.aborted || sessionStorage.getItem('token') !== token) throw new DOMException('Consulta cancelada.', 'AbortError')
    const materiais = [...privados.map((r) => ({ ...r, trilha: r.trilha || 'Sem trilha associada', status: 'RASCUNHO' })), ...publicados]
    return { trilhas, materiais, publicados: publicados.length, rascunhos: privados.length }
}

export function filtrarMateriais(materiais, busca, status, trilha) {
    const normalizar = (texto) => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR')
    const termo = normalizar(busca.trim())
    return materiais.filter((material) =>
        normalizar(material.titulo).includes(termo) &&
        (status === 'TODOS' || material.status === status) &&
        (!trilha || String(material.trilhaId) === trilha))
}
