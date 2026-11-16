import { api } from './api.js'
export const academy = (caminho, { privado = false, ...opcoes } = {}) => api('/academy' + caminho, { ...opcoes, autenticado: privado })
export const CATEGORIAS = { FRONTEND: 'Front-end', BACKEND: 'Back-end', DADOS: 'Dados', UX_DESIGN: 'UX/Design', SOFT_SKILLS: 'Soft Skills' }
export const NIVEIS = { INICIANTE: 'Iniciante', INTERMEDIARIO: 'Intermediário', AVANCADO: 'Avançado' }
export const duracao = (minutos) => minutos < 60 ? minutos + ' min' : Math.floor(minutos / 60) + 'h' + (minutos % 60 ? ' ' + minutos % 60 + 'min' : '')
export const horario = (data) => new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo' }).format(new Date(data))