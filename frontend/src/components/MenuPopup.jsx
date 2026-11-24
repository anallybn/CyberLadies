import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { User, MessageCircle, LogOut, X } from 'lucide-react'
import { secoes, secoesEducadora } from './secoes'
import './MenuPopup.css'
import { useSessao } from '../hooks/useSessao'
import { useConta } from '../hooks/useConta'
import { temPapel, sair } from '../services/sessaoService'
import './MenuEducadora.css'

// itens da conta (ficam "em breve" até as páginas existirem)
const conta = [
    { id: 'perfil', titulo: 'Perfil', descricao: 'Minha conta', icone: User, cor: 'lilas', rota: '/perfil' },
    { id: 'chat', titulo: 'Chat', descricao: 'Mensagens diretas', icone: MessageCircle, cor: 'verde', rota: '/chat' },
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

export default function MenuPopup({ ativa, onFechar, confirmarSaida }) {
    const { conta: identidade } = useSessao()
    const { conta: detalhes } = useConta()
    const navigate = useNavigate()
    const [saindo, setSaindo] = useState(false)
    const [erroLocal, setErroLocal] = useState('')
    const educadora = temPapel(identidade, 'EDUCADORA')
    const educadoraAprovada = educadora && detalhes?.statusEducadora === 'APROVADA'
    async function encerrar() {
        if (confirmarSaida && !confirmarSaida()) return
        setSaindo(true); setErroLocal('')
        try { await sair(); onFechar(); navigate('/login', { replace: true }) }
        catch (falha) { setErroLocal(falha.message) }
        finally { setSaindo(false) }
    }
    function navegar(evento) {
        if (confirmarSaida && !confirmarSaida()) { evento.preventDefault(); return }
        onFechar()
    }
    const dialogo = useRef(null)
    const pessoal = temPapel(identidade, 'USUARIA') || temPapel(identidade, 'EDUCADORA')
    const itens = [...(pessoal ? secoes : temPapel(identidade, 'EMPRESA') ? secoes.filter(s => s.id !== 'talk') : []),
        ...(educadoraAprovada ? secoesEducadora : []),
        ...(temPapel(identidade, 'USUARIA') ? [{ id: 'educadora', titulo: educadora ? 'Minha solicitação' : 'Atuar como educadora', descricao: 'Solicitação de atuação', icone: User, cor: 'lilas', rota: '/educadora' }] : []),
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
        <div className={educadora ? 'mp-fundo me-fundo' : 'mp-fundo'} onClick={onFechar}>
            <div
                className={educadora ? 'mp me' : 'mp'}
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
                        <Item key={s.id} item={s} ativo={ativa === s.id} aoClicar={navegar} />
                    ))}

                    <hr className="mp-divisor" />

                    {conta.filter(c => c.id !== 'perfil' || pessoal).map((c) => (
                        <Item key={c.id} item={c.id === 'perfil' && educadora ? { ...c, rota: null } : c} aoClicar={navegar} />
                    ))}

                    <hr className="mp-divisor" />

                    <button type="button" className="mp-item" disabled={saindo} onClick={encerrar}>
                        <span className="mp-icone mp-icone--rosa">
                            <LogOut size={20} />
                        </span>
                        <span className="mp-texto">
                            <strong>{saindo ? 'Saindo...' : 'Sair'}</strong>
                            <small>Encerrar sessão</small>
                        </span>
                    </button>
                    {erroLocal && <p role="alert">{erroLocal}</p>}
                </nav>
            </div>
        </div>
    )
}
