import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import { UserRound } from 'lucide-react'
import icone from '../assets/icone.svg'
import { useSessao } from '../hooks/useSessao'
import { useConta } from '../hooks/useConta'
import { temPapel } from '../services/sessaoService'
import { secoes, secoesEducadora } from './secoes'
import MenuPopup from './MenuPopup'
import './TopoLogado.css'

export default function TopoLogado({ ativa, confirmarSaida }) {
    const { conta } = useSessao()
    const { ultimaContaConfirmada } = useConta()
    const [menuAberto, setMenuAberto] = useState(false)
    const fecharMenu = useCallback(() => setMenuAberto(false), [])

    const usuaria = temPapel(conta, 'USUARIA')
    const educadora = temPapel(conta, 'EDUCADORA')
    const empresa = temPapel(conta, 'EMPRESA')
    const admin = temPapel(conta, 'ADMIN')
    const pessoal = usuaria || educadora
    const aprovada = educadora && ultimaContaConfirmada?.statusEducadora === 'APROVADA'
    const inicio = admin ? '/admin/educadoras' : aprovada ? '/educadora/dashboard'
        : pessoal ? '/comunidade' : empresa ? '/vagas' : '/'

    const links = [
        ...secoes.filter(secao => (pessoal || empresa) && (secao.id !== 'talk' || pessoal)),
        ...(pessoal || empresa ? [{ id: 'chat', titulo: 'CyberChat', rota: '/chat' }] : []),
        ...(aprovada ? secoesEducadora.map(secao => secao.id === 'dashboard-educadora'
            ? { ...secao, titulo: 'Painel da educadora' } : secao) : []),
        ...(usuaria ? [{ id: 'educadora', titulo: educadora ? 'Minha solicitação' : 'Atuar como educadora', rota: '/educadora' }] : []),
        ...(admin ? [{ id: 'admin', titulo: 'Solicitações', rota: '/admin/educadoras' }] : []),
    ]

    return (
        <>
            <header className="topo-logado">
                <Link className="topo-logado__marca" to={inicio} aria-label="CyberLadies">
                    <img src={icone} alt="" className="topo-logado__logo" />
                </Link>

                {links.length > 0 && (
                    <nav className="topo-logado__nav" aria-label="Seções do site">
                        {links.map(({ id, titulo, rota }) => (
                            <Link key={id} to={rota} className="topo-logado__link"
                                aria-current={ativa === id ? 'page' : undefined}>
                                {titulo}
                            </Link>
                        ))}
                    </nav>
                )}

                <button type="button" className="topo-logado__avatar" onClick={() => setMenuAberto(true)}
                    aria-label="Abrir menu da conta" aria-haspopup="dialog" aria-expanded={menuAberto}>
                    <UserRound size={18} aria-hidden="true" />
                </button>
            </header>
            {menuAberto && <MenuPopup ativa={ativa} confirmarSaida={confirmarSaida} onFechar={fecharMenu} />}
        </>
    )
}
