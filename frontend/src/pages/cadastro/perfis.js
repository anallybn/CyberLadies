import { User, AtSign, Lock, Sparkles } from 'lucide-react'

export const perfis = {
    usuaria: {
        titulo: 'Crie sua conta',
        subtitulo: 'Vagas, trilhas de estudo e comunidade para você.',
        campos: [
            {
                name: 'nome',
                label: 'Nome completo',
                tipo: 'text',
                icone: User,
                placeholder: 'Seu nome',
                autoComplete: 'name',
                obrigatorio: true,
            },
            {
                name: 'email',
                label: 'E-mail',
                tipo: 'email',
                icone: AtSign,
                placeholder: 'seuemail@exemplo.com',
                autoComplete: 'email',
                obrigatorio: true,
            },
            {
                name: 'senha',
                label: 'Senha',
                tipo: 'password',
                icone: Lock,
                placeholder: 'Min. 8 caracteres',
                autoComplete: 'new-password',
                obrigatorio: true,
                metade: true,
            },
            {
                name: 'confirmarSenha',
                label: 'Confirmar Senha',
                tipo: 'password',
                icone: Lock,
                placeholder: 'Repita a senha',
                autoComplete: 'new-password',
                obrigatorio: true,
                metade: true,
            },
            {
                name: 'competencias',
                label: 'Competências (opcional)',
                tipo: 'text',
                icone: Sparkles,
                placeholder: 'Ex: Java, UX, análise de dados',
                obrigatorio: false,
            },
        ],
    },
}