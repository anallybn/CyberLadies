import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { User, MessageCircle, LogOut, X } from 'lucide-react'
import { secoes } from './secoes'
import './MenuPopup.css'

// itens da conta (ficam "em breve" até as páginas existirem)
const conta = [
    { id: 'perfil', titulo: 'Perfil', descricao: 'Minha conta', icone: User, cor: 'lilas', rota: null },
    { id: 'chat', titulo: 'Chat', descricao: 'Mensagens diretas', icone: MessageCircle, cor: 'verde', rota: null },
]

function Item({ item, ativo, aoClicar }) {
    const Icone = item.icone

    const conteudo = (
        <>
            <span className={`mp-icone mp-icone--${item.cor}`}>
                <Icone size={20} />
            </span>
            <span className="mp-texto">
                <strong>{item.titulo}</strong>
                <small>{item.descricao}</small>
            </span>
            {!item.rota && <span className="mp-breve">Em breve</span>}
        </>
    )

    if (!item.rota) {
        return (
            <button type="button" className="mp-item" disabled>
                {conteudo}
            </button>
        )
    }

    return (
        <Link
            to={item.rota}
            className={`mp-item ${ativo ? 'mp-item--ativo' : ''}`}
            onClick={aoClicar}
        >
            {conteudo}
        </Link>
    )
}

export default function MenuPopup({ ativa, onFechar }) {
    const navigate = useNavigate()

    useEffect(() => {
        const aoApertar = (e) => {
            if (e.key === 'Escape') onFechar()
        }
        document.addEventListener('keydown', aoApertar)
        return () => document.removeEventListener('keydown', aoApertar)
    }, [onFechar])

    function sair() {
        // quando existir o login de verdade, aqui também se limpa a sessão
        localStorage.removeItem('token')
        onFechar()
        navigate('/')
    }

    return (
        <div className="mp-fundo" onClick={onFechar}>
            <div
                className="mp"
                role="dialog"
                aria-modal="true"
                aria-labelledby="mp-titulo"
                onClick={(e) => e.stopPropagation()}
            >
                <button type="button" className="mp-fechar" onClick={onFechar} aria-label="Fechar menu">
                    <X size={20} />
                </button>

                <span className="mp-tag">
                    <User size={12} /> USUÁRIA
                </span>
                <h2 id="mp-titulo" className="mp-titulo">Olá, bem-vinda!</h2>
                <p className="mp-subtitulo">O que vamos explorar hoje?</p>

                <nav className="mp-lista">
                    {secoes.map((s) => (
                        <Item key={s.id} item={s} ativo={ativa === s.id} aoClicar={onFechar} />
                    ))}

                    <hr className="mp-divisor" />

                    {conta.map((c) => (
                        <Item key={c.id} item={c} aoClicar={onFechar} />
                    ))}

                    <hr className="mp-divisor" />

                    <button type="button" className="mp-item" onClick={sair}>
                        <span className="mp-icone mp-icone--rosa">
                            <LogOut size={20} />
                        </span>
                        <span className="mp-texto">
                            <strong>Sair</strong>
                            <small>Encerrar sessão</small>
                        </span>
                    </button>
                </nav>
            </div>
        </div>
    )
}