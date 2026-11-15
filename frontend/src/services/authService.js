import { api } from './api.js'
export async function cadastrar(perfil, dados) {
    if (perfil !== 'usuaria') throw new Error('Use o cadastro de conta pessoal. Outros cadastros não estão disponíveis.')
    return api('/auth/cadastro/usuaria', { method: 'POST', dados, autenticado: false })
}
