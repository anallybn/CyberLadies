import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { User, MessageCircle, LogOut, X, LayoutDashboard, Upload, FileText, Users, GraduationCap, Layers, Briefcase } from 'lucide-react'
import { secoes } from './secoes'
import { encerrarSessao } from '../services/authService'
import { useConta } from '../hooks/useConta'
import './MenuPopup.css'
import './MenuEducadora.css'

// itens da conta (ficam "em breve" até as páginas existirem)
const conta = [
    { id: 'perfil', titulo: 'Perfil', descricao: 'Minha conta', icone: User, cor: 'lilas', rota: '/perfil' },
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

export default function MenuPopup({ ativa, onFechar, confirmarSaida }) {
    const navigate = useNavigate()
    const { conta: autenticada, carregando, erro, expirada, recarregar } = useConta()
    const menuEducadora = autenticada?.perfil === 'EDUCADORA'
    const rotuloPerfil = { CYBERLADIE: 'USUÁRIA', EDUCADORA: 'EDUCADORA', EMPRESA: 'EMPRESA' }[autenticada?.perfil] || ''
    const [saindo, setSaindo] = useState(false)
    const [erroSaida, setErroSaida] = useState('')

    useEffect(() => {
        const aoApertar = (e) => {
            if (e.key === 'Escape') onFechar()
        }
        document.addEventListener('keydown', aoApertar)
        return () => document.removeEventListener('keydown', aoApertar)
    }, [onFechar])

    async function sair() {
        if (saindo || confirmarSaida && !confirmarSaida()) return
        setSaindo(true)
        setErroSaida('')
        try {
            await encerrarSessao()
            onFechar()
            navigate('/')
        } catch (erro) {
            setErroSaida(erro.message)
        } finally {
            setSaindo(false)
        }
    }

    if (!autenticada) return <div className="mp-fundo" onClick={onFechar}>
        <div className="mp" role="dialog" aria-modal="true" aria-labelledby="mp-titulo" onClick={(evento) => evento.stopPropagation()}>
            <button type="button" className="mp-fechar" onClick={onFechar} aria-label="Fechar menu"><X size={20} /></button>
            <h2 id="mp-titulo" className="mp-titulo">Menu da conta</h2>
            {carregando ? <p role="status" className="mp-subtitulo">Confirmando sua conta…</p> : erro ? <>
                <p role="alert" className="mp-subtitulo">{erro}</p>
                <button type="button" className="mp-item" onClick={recarregar}>Tentar novamente</button>
            </> : <>
                <p className="mp-subtitulo">{expirada ? 'Sua sessão expirou. Entre novamente.' : 'Entre para acessar o menu da sua conta.'}</p>
                <Link className="mp-item" to="/login" onClick={onFechar}>Entrar</Link>
            </>}
        </div>
    </div>
    return (
        <div className={menuEducadora ? "mp-fundo me-fundo" : "mp-fundo"} onClick={onFechar}>
            <div
                className={menuEducadora ? "mp me" : "mp"}
                role="dialog"
                aria-modal="true"
                aria-labelledby="mp-titulo"
                onClick={(e) => e.stopPropagation()}
            >
                <button type="button" className="mp-fechar" onClick={onFechar} aria-label="Fechar menu">
                    <X size={20} />
                </button>

                <span className="mp-tag">
                    {menuEducadora ? <GraduationCap size={13} /> : <User size={12} />} {menuEducadora ? "EDUCADORA" : rotuloPerfil}
                </span>
                <h2 id="mp-titulo" className="mp-titulo">{menuEducadora ? "Painel da educadora" : "Olá, bem-vinda!"}</h2>
                <p className="mp-subtitulo">{menuEducadora ? "Gerencie seus conteúdos" : "O que vamos explorar hoje?"}</p>

                {erroSaida && <p role="alert">{erroSaida}</p>}
                <nav className="mp-lista">
                    {menuEducadora ? <>
                        <div className="me-principal">
                            {[
                                { id: 'dashboard-educadora', titulo: 'Dashboard', descricao: 'Visão geral', icone: LayoutDashboard, rota: '/educadora/dashboard' },
                                { id: 'enviar', titulo: 'Enviar Material', descricao: 'Novo conteúdo', icone: Upload, rota: '/educadora/enviar-material' },
                                { id: 'materiais', titulo: 'Meus Materiais', descricao: 'Conteúdos publicados', icone: FileText, rota: '/educadora/meus-materiais' },
                                { id: 'alunas', titulo: 'Minhas Alunas', descricao: 'Alunas inscritas', icone: Users, rota: '/educadora/minhas-alunas' },
                                { id: 'chat', titulo: 'Chat', descricao: 'Mensagens diretas', icone: MessageCircle, rota: null },
                                { id: 'talk', titulo: 'CyberTalk', descricao: 'Comunidade', icone: MessageCircle, rota: '/comunidade' },
                            ].map((item) => <Item key={item.id} item={{ ...item, cor: 'lilas' }} ativo={ativa === item.id} aoClicar={onFechar} />)}
                        </div>
                        <div className="me-secundario" aria-label="Outros espaços">
                            <Item item={{ titulo: 'CyberAcademy', descricao: 'Estudar e aprender', icone: Layers, cor: 'lilas', rota: '/academy' }} ativo={ativa === 'academy'} aoClicar={onFechar} />
                            <Item item={{ titulo: 'CyberVagas', descricao: 'Consultar oportunidades', icone: Briefcase, cor: 'lilas', rota: '/vagas' }} ativo={ativa === 'vagas'} aoClicar={onFechar} />
                            <Item item={{ titulo: 'Perfil', descricao: 'Perfil profissional', icone: User, cor: 'lilas', rota: null }} />
                        </div>
                    </> : <>
                        {secoes.map((s) => <Item key={s.id} item={s} ativo={ativa === s.id} aoClicar={onFechar} />)}
                        <hr className="mp-divisor" />
                        {conta.filter((c) => c.id !== 'perfil' || autenticada?.perfil === 'CYBERLADIE').map((c) => <Item key={c.id} item={c} ativo={ativa === c.id} aoClicar={onFechar} />)}
                        <hr className="mp-divisor" />
                    </>}                    <button type="button" className={menuEducadora ? "mp-item me-sair" : "mp-item"} disabled={saindo} aria-busy={saindo} onClick={sair}>
                        <span className="mp-icone mp-icone--rosa">
                            <LogOut size={20} />
                        </span>
                        <span className="mp-texto">
                            <strong>{saindo ? 'Saindo…' : 'Sair'}</strong>
                            <small>Encerrar sessão</small>
                        </span>
                    </button>
                </nav>
            </div>
        </div>
    )
}