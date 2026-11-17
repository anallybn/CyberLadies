import { api } from './api.js'
export function talk(caminho, method = 'GET', dados, binario = false) {
    return api(caminho, { method, dados, binario })
}
