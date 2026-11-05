import { useEffect, useState } from "react";
import { BookOpen, Building2, PawPrint, Heart, MessageCircle, Share2 } from 'lucide-react'
import icone from '../assets/icone.svg'
import Circuitos from '../components/Circuitos'
import TopoLogado from '../components/TopoLogado'
import PostTalk from '../components/PostTalk'
import { talk } from '../services/talkApi'
import {MODO_TESTE, carregarComunidade, publicarPost} from '../services/comunidadeService'
import './Comunidade.css'

const abas = [
    { id: 'todos', label: 'Para você' },
    { id: 'usuaria', label: 'Usuárias'},
    { id: 'educadora', label: 'Educadoras' },
    { id: 'empresa', label: 'Empresas' },
    { id: 'vaga', label: 'Vagas' },
]


const iconesPapel = {
    'Usuária': PawPrint, 
    'Educadora': BookOpen, 
    'Empresa': Building2, 
}

function Post({ post }) {
    const [curtido, setCurtido] = useState(false)
    const IconePapel = iconesPapel[post.papel] || PawPrint

    return (
        <article className="cm-cartao cm-post">
            <header className="cm-post__topo">
                <div className="cm-avatar">{post.autora[0]}</div>
                <div>
                    <div className="cm-post__nome">
                        <strong>{post.autora}</strong>
                        <span className={`cm-selo cm-selo--${iconesPapel[post.papel] || 'lilas'}`}
                            title={post.papel}
                            aria-label={post.papel}
                            >
                                <IconePapel size={14} />
                        </span>
                    </div>
                    <p className="cm-post__info">{post.cargo} · {post.tempo}</p>
                </div>
            </header>

            <p className="cm-post__texto">{post.texto}</p>

            {post.tags.length > 0 && (
                <div className="cm-post__tags">
                    {post.tags.map((t) => <span key={t}>#{t}</span>)}
                </div>
            )}

            <footer className="cm-post__acoes">
                <button
                    type="button"
                    className={curtido ? 'ativo' : ''}
                    aria-pressed={curtido}
                    onClick={() => setCurtido(!curtido)}
                >
                    <Heart size={16} fill={curtido ? 'currentColor' : 'none'} />
                    {post.curtidas + (curtido ? 1 : 0)}
                </button>
                <button type="button">
                    <MessageCircle size={16} />
                    {post.comentarios}
                </button>
                <button type="button">
                    <Share2 size={16} /> Compartilhar
                </button>
            </footer>
        </article>
    )
}

export default function Comunidade() {
    const [dados, setDados] = useState(null)
    const [posts, setPosts] = useState([])
    const [carregando, setCarregando] = useState(true)
    const [erro, setErro] = useState(null)
    const [tentativa, setTentativa] = useState(0)

    const [aba, setAba] = useState('todos')
    const [texto, setTexto] = useState('')
    const [publicando, setPublicando] = useState(false)
    const [aviso, setAviso] = useState('')
    const [erroPublicacao, setErroPublicacao] = useState(null)
    const [conectados, setConectando] = useState([])

    useEffect(() => {
    let ativo = true

    carregarComunidade()
        .then((resultado) => {
            if (!ativo) return
            setDados(resultado)
            setPosts(resultado.posts)
        })
        .catch((falha) => {
            if (ativo) setErro(falha.message)
        })
        .finally(() => {
            if (ativo) setCarregando(false)
        })

    return () => { ativo = false }
}, [tentativa])

   

    function tentarDeNovo() {
        setErro(null)
        setCarregando(true)
        setTentativa((t) => t + 1)
    }

    async function publicar(evento) {
        evento.preventDefault()
        const conteudo = texto.trim()
        if (!conteudo || publicando) return

        setPublicando(true)
        setErroPublicacao(null)
        try {
            const post = await publicarPost(conteudo)
            setPosts((atuais) => [post, ...atuais])
            setTexto('')
            setAviso('Publicação feita!')
        } catch (falha) {
            setErroPublicacao(falha.message)
        } finally {
            setPublicando(false)
        }
    }

    async function aderir() {
        setPublicando(true)
        setErroPublicacao(null)
        try {
            await talk('/comunidade/adesao', 'PUT')
            tentarDeNovo()
        } catch (falha) {
            setErroPublicacao(falha.message)
        } finally {
            setPublicando(false)
        }
    }

    async function sair() {
        try {
            await talk('/auth/sessao', 'DELETE')
            sessionStorage.removeItem('token')
            window.location.replace('/login?perfil=usuaria')
        } catch (falha) {
            setErroPublicacao(falha.message)
        }
    }

    function conectar(id) {
        //TODO: CHAMAR A API DE CONEXÕES QUANDO O BACK TIVER ESSA ROTA
        setConectando((atuais) => [...atuais, id])
    }

    const { usuaria, grupos, sugestoes, eventos } = dados || {}
    const inicial = usuaria?.nome?.[0] || ''
    const visiveis = aba === 'todos' ? posts : posts.filter((p) => p.categoria === aba)

    return (
        <div className="cm">
            <TopoLogado ativa="talk" inicial={inicial} />
            <Circuitos />

            {carregando && (
                <div className="cm-estado" role="status">
                    <span className="cm-spinner" />
                    Carregando comunidade...
                </div>
            )}

            {!carregando && erro && (
                <div className="cm-estado">
                    <p>{erro}</p>
                    <button type="button" className="cm-botao" onClick={tentarDeNovo}>
                        Tentar novamente
                    </button>
                </div>
            )}

            {!carregando && !erro && dados && !MODO_TESTE && !usuaria.membro && (
                <section className="cm-estado"><h1>CyberTalk</h1>
                    <button className="cm-botao" disabled={publicando} onClick={aderir}>Participar da comunidade</button>
                    {erroPublicacao && <p role="alert">{erroPublicacao}</p>}
                </section>
            )}
            {!carregando && !erro && dados && (MODO_TESTE || usuaria.membro) && (
                <div className={MODO_TESTE ? "cm-grade" : "cm-grade cm-grade--talk"}>
                    <main className="cm-feed">
                        {!MODO_TESTE && <header className="cm-talk-topo"><h1>CyberTalk</h1>
                            <button type="button" className="cm-botao cm-botao--contorno" onClick={sair}>Sair</button>
                        </header>}
                        <form className="cm-cartao cm-publicar" onSubmit={publicar}>
                            <div className="cm-avatar">{inicial}</div>
                            <input
                                value={texto}
                                onChange={(e) => setTexto(e.target.value)}
                                placeholder="Compartilhe algo com a comunidade..."
                                aria-label="Nova publicação"
                                maxLength={500}
                            />
                            <button
                                type="submit"
                                className="cm-botao"
                                disabled={!texto.trim() || publicando}
                            >
                                {publicando ? 'Publicando...' : 'Publicar'}
                            </button>
                        </form>

                        {aviso && <p role="status">{aviso}</p>}
                        {erroPublicacao && <p className="cm-erro" role="alert">{erroPublicacao}</p>}

                        {MODO_TESTE && <nav className="cm-abas" aria-label="Filtrar publicações">
                            {abas.map((a) => (
                                <button
                                    key={a.id}
                                    type="button"
                                    className={aba === a.id ? 'ativa' : ''}
                                    onClick={() => setAba(a.id)}
                                >
                                    {a.label}
                                </button>
                            ))}
                        </nav>}

                        {visiveis.length === 0 ? (
                            <div className="cm-cartao cm-vazio">
                                <img src={icone} alt="" />
                                <p>Ainda não há publicações aqui.</p>
                                <small>Que tal ser a primeira?</small>
                            </div>
                        ) : (
                            visiveis.map((p) => MODO_TESTE ? <Post key={p.id} post={p} /> :
                                <PostTalk key={p.id} post={p}
                                    atualizar={novo => setPosts(lista => lista.map(item => item.id === novo.id ? novo : item))}
                                    remover={id => { setPosts(lista => lista.filter(item => item.id !== id)); setAviso('Publicação excluída.') }} />)
                        )}
                    </main>

                    {MODO_TESTE && <aside className="cm-coluna cm-coluna--dir">
                        <section className="cm-cartao">
                            <h2 className="cm-titulo">Meus grupos</h2>
                            <ul className="cm-lista">
                                {grupos.map((g) => (
                                    <li key={g.id}>
                                        <span className="cm-sigla">{g.sigla}</span>
                                        <div>
                                            <strong>{g.nome}</strong>
                                            <p>{g.membros}</p>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </section>

                        <section className="cm-cartao">
                            <h2 className="cm-titulo">Sugestões para você</h2>
                            <ul className="cm-lista">
                                {sugestoes.map((s) => (
                                    <li key={s.id}>
                                        <div className="cm-avatar">{s.nome[0]}</div>
                                        <div>
                                            <strong>{s.nome}</strong>
                                            <p>{s.papel} · {s.info}</p>
                                        </div>
                                        <button
                                            type="button"
                                            className="cm-botao cm-botao--contorno"
                                            disabled={conectados.includes(s.id)}
                                            onClick={() => conectar(s.id)}
                                        >
                                            {conectados.includes(s.id) ? 'Enviado' : 'Conectar'}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        </section>

                        <section className="cm-cartao">
                            <h2 className="cm-titulo">Próximos eventos</h2>
                            <ul className="cm-lista">
                                {eventos.map((ev) => (
                                    <li key={ev.id}>
                                        <span className="cm-data"><small>{ev.mes}</small>{ev.dia}</span>
                                        <div>
                                            <strong>{ev.titulo}</strong>
                                            <p>{ev.info}</p>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </section>
                    </aside>}
                </div> 
            )}
        </div>
    )
}
        