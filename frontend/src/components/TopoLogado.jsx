import { useState } from 'react'
import { Link } from 'react-router-dom'
import { UserRound } from 'lucide-react'
import icone from '../assets/icone.svg'
import { secoes } from './secoes'
import MenuPopup from './MenuPopup'
import './TopoLogado.css'

// topo das páginas de dentro do site (depois do login)
export default function TopoLogado({ ativa, inicial }) {
    const [menuAberto, setMenuAberto] = useState(false)

    return (
        <>
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
                                {s.titulo}
                            </Link>
                        ) : (
                            <button
                                key={s.id}
                                type="button"
                                className="topo-logado__link topo-logado__link--breve"
                                disabled
                                title="Em breve"
                            >
                                {s.titulo}
                            </button>
                        )
                    )}
                </nav>

                <button
                    type="button"
                    className="topo-logado__avatar"
                    onClick={() => setMenuAberto(true)}
                    aria-label="Abrir menu da conta"
                    aria-haspopup="dialog"
                    aria-expanded={menuAberto}
                >
                    {inicial || <UserRound size={18} />}
                </button>
            </header>

            {menuAberto && <MenuPopup ativa={ativa} onFechar={() => setMenuAberto(false)} />}
        </>
    )
}