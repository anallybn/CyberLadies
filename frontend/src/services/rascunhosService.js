import { api } from './api.js'
export const rascunhos = (caminho = '', { operacao, ...opcoes } = {}) => api('/educadora/materiais/rascunhos' + caminho, { ...opcoes, headers: operacao ? { 'Idempotency-Key': operacao } : {} })
