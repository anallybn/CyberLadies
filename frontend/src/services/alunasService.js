import { api } from './api.js'
export const consultarAlunas = (caminho = '', signal) => api('/educadora/alunas' + caminho, { signal })
