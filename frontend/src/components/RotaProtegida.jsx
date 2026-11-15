import { useEffect } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useSessao } from '../hooks/useSessao'
import { atualizarSessao, temPapel } from '../services/sessaoService'
import AreaConta from './AreaConta'

export default function RotaProtegida({ papeis, children }) {
    const sessao = useSessao()
    const { pathname, search } = useLocation()
    useEffect(() => { atualizarSessao().catch(() => {}) }, [pathname])
    if (sessao.erro) return <AreaConta titulo="Não foi possível verificar sua sessão">
        <p role="alert">{sessao.erro}</p>
        <button className="cm-botao" onClick={() => atualizarSessao().catch(() => {})}>Tentar novamente</button>
    </AreaConta>
    if (!sessao.verificada) return <AreaConta titulo="Verificando sessão"><p role="status">Carregando sua conta...</p></AreaConta>
    if (!sessao.conta) {
        const params = new URLSearchParams()
        if (sessao.expirada) params.set('motivo', 'sessao-expirada')
        const post = new URLSearchParams(search).get('post')
        if (pathname === '/comunidade' && /^\d+$/.test(post || '')) params.set('post', post)
        return <Navigate to={'/login' + (params.size ? '?' + params.toString() : '')} replace />
    }
    if (!papeis.some(papel => temPapel(sessao.conta, papel))) return <AreaConta titulo="Acesso negado">
        <p role="alert">Sua conta não tem permissão para acessar esta área.</p>
        <Link to={temPapel(sessao.conta, 'ADMIN') ? '/admin/educadoras' : '/comunidade'}>Voltar à minha área</Link>
    </AreaConta>
    return children
}
