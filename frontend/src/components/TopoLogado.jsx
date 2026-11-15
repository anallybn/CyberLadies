import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useSessao } from '../hooks/useSessao'
import { sair, temPapel } from '../services/sessaoService'
import { UserRound } from 'lucide-react'
import icone from '../assets/icone.svg'
import './TopoLogado.css'

const secoes = [
    { id: 'academy', nome: 'CyberAcademy', rota: null },
    { id: 'vagas', nome: 'CyberVagas', rota: null },
    { id: 'talk', nome: 'CyberTalk', rota: '/comunidade' },
]

// topo das páginas de dentro do site (depois do login)
export default function TopoLogado({ ativa, inicial }) {
    const { conta } = useSessao()
    const navigate = useNavigate()
    const [erro, setErro] = useState('')
    const [saindo, setSaindo] = useState(false)
    const pessoal = temPapel(conta, 'USUARIA') || temPapel(conta, 'EDUCADORA')
    const admin = temPapel(conta, 'ADMIN')
    async function encerrar() {
        setSaindo(true); setErro('')
        try { await sair(); navigate('/login', { replace: true }) }
        catch (falha) { setErro(falha.message) }
        finally { setSaindo(false) }
    }
    return (
        <header className="topo-logado">
            <Link to={admin ? '/admin/educadoras' : '/comunidade'} aria-label="CyberLadies">
                <img src={icone} alt="" className="topo-logado__logo" />
            </Link>

            <nav className="topo-logado__nav" aria-label="Seções do site">
                {pessoal && secoes.map((s) =>
                    s.rota ? (
                        <Link
                            key={s.id}
                            to={s.rota}
                            className={`topo-logado__link ${ativa === s.id ? 'ativa' : ''}`}
                            aria-current={ativa === s.id ? 'page' : undefined}
                        >
                            {s.nome}
                        </Link>
                    ) : (
                        <button
                            key={s.id}
                            type="button"
                            className="topo-logado__link"
                            disabled
                            title="Em breve"
                        >
                            {s.nome}
                        </button>
                    )
                )}
                {temPapel(conta, 'USUARIA') && <Link to="/educadora" className={`topo-logado__link ${ativa === 'educadora' ? 'ativa' : ''}`}>Atuar como educadora</Link>}
                {admin && <Link to="/admin/educadoras" className={`topo-logado__link ${ativa === 'admin' ? 'ativa' : ''}`}>Solicitações</Link>}
            </nav>
            {conta && <button type="button" className="topo-logado__link" disabled={saindo} onClick={encerrar}>{saindo ? 'Saindo...' : 'Sair'}</button>}
            {erro && <p role="alert" className="topo-logado__erro">{erro}</p>}
            <div className="topo-logado__avatar" title="Meu perfil">
                {inicial || <UserRound size={18} />}
            </div>
        </header>
    )
}
