import { useEffect, useState } from "react";
import { Link, useSearchParams } from 'react-router-dom'
import icone from '../assets/icone.svg'
import Circuitos from '../components/Circuitos'
import TopoLogado from '../components/TopoLogado'
import PostTalk from '../components/PostTalk'
import { talk } from '../services/talkApi'
import { carregarComunidade, publicarPost } from '../services/comunidadeService'
import './Comunidade.css'

const ABAS = [
    { id: 'todos', rotulo: 'Para você' },
    { id: 'usuaria', rotulo: 'Usuárias' },
    { id: 'educadora', rotulo: 'Educadoras' },
    { id: 'empresa', rotulo: 'Empresas' },
    { id: 'vaga', rotulo: 'Vagas' },
]

export default function Comunidade() {
    const [dados, setDados] = useState(null)
    const [posts, setPosts] = useState([])
    const [carregando, setCarregando] = useState(true)
    const [erro, setErro] = useState(null)
    const [tentativa, setTentativa] = useState(0)

    const [params] = useSearchParams()
    const postId = params.get('post')
    const [arquivo, setArquivo] = useState(null)
    const [texto, setTexto] = useState('')
    const [publicando, setPublicando] = useState(false)
    const [aviso, setAviso] = useState('')
    const [erroPublicacao, setErroPublicacao] = useState(null)
    const [aba, setAba] = useState('todos')

    useEffect(() => {
        let ativo = true

        carregarComunidade(postId)
            .then((resultado) => {
                if (!ativo) return
                setErro(null)
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
    }, [tentativa, postId])

    function tentarDeNovo() {
        setErro(null)
        setCarregando(true)
        setTentativa((t) => t + 1)
    }

    async function publicar(evento) {
        evento.preventDefault()
        const form = evento.currentTarget
        const conteudo = texto.trim()
        if ((!conteudo && !arquivo) || publicando) return

        setPublicando(true)
        setErroPublicacao(null)
        try {
            const post = await publicarPost(conteudo, arquivo)
            setPosts((atuais) => [post, ...atuais])
            setTexto('')
            setArquivo(null)
            form.reset()
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

    const usuaria = dados?.usuaria
    const inicial = usuaria?.nome?.[0] || ''

    // Só o que o backend enviar. Se não vier, a lista aparece vazia.
    const grupos = dados?.grupos ?? []
    const sugestoes = dados?.sugestoes ?? []
    const eventos = dados?.eventos ?? []

    // Filtro das abas (usa o campo "perfil" do post, se o backend enviar)
    const postsVisiveis = aba === 'todos' ? posts : posts.filter((p) => p.perfil === aba)

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
                    {postId && <Link to="/comunidade">Voltar à comunidade</Link>}
                    <button type="button" className="cm-botao" onClick={tentarDeNovo}>
                        Tentar novamente
                    </button>
                </div>
            )}

            {!carregando && !erro && dados && !usuaria.membro && (
                <section className="cm-estado">
                    <h1>CyberTalk</h1>
                    <button className="cm-botao" disabled={publicando} onClick={aderir}>
                        Participar da comunidade
                    </button>
                    {erroPublicacao && <p role="alert">{erroPublicacao}</p>}
                </section>
            )}

            {!carregando && !erro && dados && usuaria.membro && (
                <div className="cm-grade">
                    <main className="cm-feed">
                        {postId && <Link to="/comunidade">Ver todas as publicações</Link>}

                        {!postId && (
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
                                    disabled={(!texto.trim() && !arquivo) || publicando}
                                >
                                    {publicando ? 'Publicando...' : 'Publicar'}
                                </button>
                                <label className="cm-anexar">
                                    Anexar foto ou arquivo (até 5 MB)
                                    <input
                                        type="file"
                                        onChange={(e) => {
                                            const escolhido = e.target.files[0]
                                            if (escolhido?.size > 5 * 1024 * 1024 || escolhido?.size === 0) {
                                                setErroPublicacao('Escolha um arquivo entre 1 byte e 5 MB.')
                                                e.target.value = ''
                                                setArquivo(null)
                                            } else {
                                                setErroPublicacao(null)
                                                setArquivo(escolhido || null)
                                            }
                                        }}
                                    />
                                </label>
                            </form>
                        )}

                        {!postId && (
                            <div className="cm-abas" role="tablist">
                                {ABAS.map((a) => (
                                    <button
                                        key={a.id}
                                        type="button"
                                        role="tab"
                                        aria-selected={aba === a.id}
                                        className={aba === a.id ? 'ativa' : ''}
                                        onClick={() => setAba(a.id)}
                                    >
                                        {a.rotulo}
                                    </button>
                                ))}
                            </div>
                        )}

                        {aviso && <p role="status">{aviso}</p>}
                        {erroPublicacao && <p className="cm-erro" role="alert">{erroPublicacao}</p>}

                        {postsVisiveis.length === 0 ? (
                            <div className="cm-cartao cm-vazio">
                                <img src={icone} alt="" />
                                <p>Ainda não há publicações aqui.</p>
                                <small>Que tal ser a primeira?</small>
                            </div>
                        ) : (
                            postsVisiveis.map((p) => (
                                <PostTalk
                                    key={p.id}
                                    post={p}
                                    atualizar={(novo) =>
                                        setPosts((lista) => lista.map((item) => (item.id === novo.id ? novo : item)))
                                    }
                                    remover={(id) => {
                                        setPosts((lista) => lista.filter((item) => item.id !== id))
                                        setAviso('Publicação excluída.')
                                    }}
                                />
                            ))
                        )}
                    </main>

                    <aside className="cm-coluna">
                        <section className="cm-cartao">
                            <h2 className="cm-titulo">Meus grupos</h2>
                            {grupos.length === 0 ? (
                                <p className="cm-lista-vazia">Você ainda não participa de grupos.</p>
                            ) : (
                                <ul className="cm-lista">
                                    {grupos.map((g) => (
                                        <li key={g.id}>
                                            <span className="cm-sigla">{g.sigla || g.nome?.slice(0, 2)}</span>
                                            <div>
                                                <strong>{g.nome}</strong>
                                                {g.membros != null && <p>{g.membros} membros</p>}
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>

                        <section className="cm-cartao">
                            <h2 className="cm-titulo">Sugestões para você</h2>
                            {sugestoes.length === 0 ? (
                                <p className="cm-lista-vazia">Nenhuma sugestão por enquanto.</p>
                            ) : (
                                <ul className="cm-lista">
                                    {sugestoes.map((s) => (
                                        <li key={s.id}>
                                            <div className="cm-avatar">{s.nome?.[0]}</div>
                                            <div>
                                                <strong>{s.nome}</strong>
                                                {s.descricao && <p>{s.descricao}</p>}
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>

                        <section className="cm-cartao">
                            <h2 className="cm-titulo">Próximos eventos</h2>
                            {eventos.length === 0 ? (
                                <p className="cm-lista-vazia">Nenhum evento marcado.</p>
                            ) : (
                                <ul className="cm-lista">
                                    {eventos.map((ev) => (
                                        <li key={ev.id}>
                                            <div className="cm-data">
                                                <span>{ev.dia}</span>
                                                <small>{ev.mes}</small>
                                            </div>
                                            <div>
                                                <strong>{ev.titulo}</strong>
                                                {ev.local && <p>{ev.local}</p>}
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>

                        <button type="button" className="cm-botao cm-botao--contorno" onClick={sair}>
                            Sair
                        </button>
                    </aside>
                </div>
            )}
        </div>
    )
}
        