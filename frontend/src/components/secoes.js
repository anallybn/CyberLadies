import { Layers, Briefcase, MessageSquare, LayoutDashboard, Upload, FileText, Users } from 'lucide-react'

export const secoes = [
    { id: 'academy', titulo: 'CyberAcademy', descricao: 'Trilhas & conteúdos', icone: Layers, cor: 'azul', rota: '/academy' },
    { id: 'vagas', titulo: 'CyberVagas', descricao: 'Oportunidades para você', icone: Briefcase, cor: 'celeste', rota: '/vagas' },
    { id: 'talk', titulo: 'CyberTalk', descricao: 'Comunidade e Networking', icone: MessageSquare, cor: 'rosa', rota: '/comunidade' },
]

export const secoesEducadora = [
    { id: 'dashboard-educadora', titulo: 'Dashboard', descricao: 'Visão geral', icone: LayoutDashboard, cor: 'lilas', rota: '/educadora/dashboard' },
    { id: 'enviar', titulo: 'Enviar Material', descricao: 'Novo conteúdo', icone: Upload, cor: 'lilas', rota: '/educadora/enviar-material' },
    { id: 'materiais', titulo: 'Meus Materiais', descricao: 'Conteúdos publicados e rascunhos', icone: FileText, cor: 'lilas', rota: '/educadora/meus-materiais' },
    { id: 'alunas', titulo: 'Minhas Alunas', descricao: 'Inscrições e progresso', icone: Users, cor: 'lilas', rota: '/educadora/minhas-alunas' },
]
