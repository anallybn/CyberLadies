import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { User, MessageCircle, LogOut, X } from 'lucide-react'
import { secoes } from './secoes'
import './MenuPopup.css'
import { useSessao } from '../hooks/useSessao'
import { temPapel } from '../services/sessaoService'

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

export default function MenuPopup({ ativa, onFechar, aoSair, saindo, erro }) {
    const { conta: identidade } = useSessao()
    const dialogo = useRef(null)
    const pessoal = temPapel(identidade, 'USUARIA') || temPapel(identidade, 'EDUCADORA')
    const itens = [...(pessoal ? secoes : []),
        ...(temPapel(identidade, 'USUARIA') ? [{ id: 'educadora', titulo: 'Atuar como educadora', descricao: 'Minha solicitação', icone: User, cor: 'lilas', rota: '/educadora' }] : []),
        ...(temPapel(identidade, 'ADMIN') ? [{ id: 'admin', titulo: 'Solicitações', descricao: 'Análise de educadoras', icone: User, cor: 'lilas', rota: '/admin/educadoras' }] : [])]

    useEffect(() => {
        const anterior = document.activeElement
        dialogo.current.querySelector('button').focus()
        const aoApertar = (e) => {
            if (e.key === 'Escape') onFechar()
            if (e.key === 'Tab') {
                const itens = [...dialogo.current.querySelectorAll('a[href], button:not(:disabled)')]
                const primeiro = itens[0], ultimo = itens.at(-1)
                if (e.shiftKey && document.activeElement === primeiro) { e.preventDefault(); ultimo.focus() }
                else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primeiro.focus() }
            }
        }
        document.addEventListener('keydown', aoApertar)
        return () => { document.removeEventListener('keydown', aoApertar); anterior?.focus() }
    }, [onFechar])

    return (
        <div className="mp-fundo" onClick={onFechar}>
            <div
                className="mp"
                ref={dialogo}
                role="dialog"
                aria-modal="true"
                aria-labelledby="mp-titulo"
                onClick={(e) => e.stopPropagation()}
            >
                <button type="button" className="mp-fechar" onClick={onFechar} aria-label="Fechar menu">
                    <X size={20} />
                </button>

                <span className="mp-tag">
                    <User size={12} /> {identidade?.papeis?.join(' / ')}
                </span>
                <h2 id="mp-titulo" className="mp-titulo">Olá, bem-vinda!</h2>
                <p className="mp-subtitulo">O que vamos explorar hoje?</p>

                <nav className="mp-lista">
                    {itens.map((s) => (
                        <Item key={s.id} item={s} ativo={ativa === s.id} aoClicar={onFechar} />
                    ))}

                    <hr className="mp-divisor" />

                    {conta.map((c) => (
                        <Item key={c.id} item={c} aoClicar={onFechar} />
                    ))}

                    <hr className="mp-divisor" />

                    <button type="button" className="mp-item" disabled={saindo} onClick={aoSair}>
                        <span className="mp-icone mp-icone--rosa">
                            <LogOut size={20} />
                        </span>
                        <span className="mp-texto">
                            <strong>{saindo ? 'Saindo...' : 'Sair'}</strong>
                            <small>Encerrar sessão</small>
                        </span>
                    </button>
                    {erro && <p role="alert">{erro}</p>}
                </nav>
            </div>
        </div>
    )
}
