export function resolverBaseApi(valor = '/api') {
    const base = (valor || '/api').trim().replace(/\/+$/, '')
    if (base === '/api') return base
    const url = new URL(base)
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash || !['/', '/api'].includes(url.pathname)) throw new Error('VITE_API_URL deve apontar para a API sem credenciais ou parâmetros.')
    return url.origin + '/api'
}
export const API_BASE_URL = resolverBaseApi(import.meta.env?.VITE_API_URL)
