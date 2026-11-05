import { Link } from 'react-router-dom'
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
    return (
        <header className="topo-logado">
            <Link to="/comunidade" aria-label="CyberLadies">
                <img src={icone} alt="" className="topo-logado__logo" />
            </Link>

            <nav className="topo-logado__nav" aria-label="Seções do site">
                {secoes.map((s) =>
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
            </nav>

            <div className="topo-logado__avatar" title="Meu perfil">
                {inicial || <UserRound size={18} />}
            </div>
        </header>
    )
}