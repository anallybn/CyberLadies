import { Layers, Briefcase, MessageSquare } from 'lucide-react'

export const secoes = [
    { id: 'academy', titulo: 'CyberAcademy', descricao: 'Trilhas & conteúdos', icone: Layers, cor: 'azul', rota: null },
    { id: 'vagas', titulo: 'CyberVagas', descricao: 'Oportunidades para você', icone: Briefcase, cor: 'celeste', rota: '/vagas' },
    { id: 'talk', titulo: 'CyberTalk', descricao: 'Comunidade e Networking', icone: MessageSquare, cor: 'rosa', rota: '/comunidade' },
]