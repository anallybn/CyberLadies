import { useState } from 'react'
import { Heart, MessageCircle, Share2 } from 'lucide-react'
import AnexoTalk from './AnexoTalk'
import { talk } from '../services/talkApi'

export default function PostTalk({ post, atualizar, remover }) {
    const [editando, editar] = useState(false)
    const [texto, escrever] = useState(post.texto)
    const [comentarios, listar] = useState(null)
    const [comentario, comentar] = useState('')
    const [ocupado, aguardar] = useState(false)
    const [aviso, avisar] = useState('')
    const [linkManual, setLinkManual] = useState('')
    const rota = '/posts/' + post.id

    async function executar(acao) {
        if (ocupado) return
        aguardar(true)
        avisar('')
        try { await acao() }
        catch (erro) { avisar(erro.message) }
        finally { aguardar(false) }
    }

    async function compartilhar() {
        const link = new URL('/comunidade', window.location.origin)
        link.searchParams.set('post', post.id)
        try {
            await navigator.clipboard.writeText(link.href)
            avisar('Link copiado! Envie para outra pessoa da comunidade.')
        } catch {
            setLinkManual(link.href)
        }
    }

    async function salvar() {
        atualizar(await talk(rota, 'PUT', { texto }))
        editar(false)
        avisar('Edições salvas!')
    }

    async function enviarComentario() {
        const novo = await talk(rota + '/comentarios', 'POST', { texto: comentario })
        listar([...comentarios, novo])
        comentar('')
        atualizar(await talk(rota))
    }

    return <article className="cm-cartao cm-post">
        <header className="cm-post__topo">
            <span className="cm-avatar">{post.autora[0]}</span>
            <div><strong>{post.autora}</strong><p className="cm-post__info">
                {new Date(post.criadoEm).toLocaleString('pt-BR')}
            </p></div>
        </header>
        {editando ? <form onSubmit={e => { e.preventDefault(); executar(salvar) }}>
            <textarea aria-label="Editar publicação" maxLength={500} value={texto}
                onChange={e => escrever(e.target.value)} />
            <button disabled={ocupado || !texto.trim()}>Salvar</button>
            <button type="button" disabled={ocupado} onClick={() => editar(false)}>Cancelar</button>
        </form> : <p className="cm-post__texto">{post.texto}</p>}
        {post.anexoNome && <AnexoTalk post={post} />}
        <footer className="cm-post__acoes">
            <button disabled={ocupado} aria-pressed={post.curtido} aria-label="Curtir publicação"
                onClick={() => executar(async () => atualizar(await talk(rota + '/curtida', post.curtido ? 'DELETE' : 'PUT')))}>
                <Heart size={16} fill={post.curtido ? 'currentColor' : 'none'} />{post.curtidas}
            </button>
            <button disabled={ocupado} aria-expanded={comentarios !== null} aria-label="Comentários"
                onClick={() => executar(async () => listar(comentarios ? null : await talk(rota + '/comentarios')))}>
                <MessageCircle size={16} />{post.comentarios}
            </button>
            <button type="button" onClick={compartilhar}><Share2 size={16} /> Compartilhar</button>
            {post.propria && <>
                <button disabled={ocupado} onClick={() => { escrever(post.texto); editar(true) }}>Editar</button>
                <button disabled={ocupado} onClick={() => {
                    if (window.confirm('Excluir esta publicação e seus comentários?'))
                        executar(async () => { await talk(rota, 'DELETE'); remover(post.id) })
                }}>Excluir</button>
            </>}
        </footer>
        {linkManual && <label>Copie o link <input readOnly value={linkManual} onFocus={e => e.target.select()} /></label>}
        {comentarios && <section aria-label="Comentários">
            {comentarios.length === 0 && <p>Seja a primeira a comentar.</p>}
            {comentarios.map(c => <p key={c.id}><strong>{c.autora}</strong> {c.texto}</p>)}
            <form onSubmit={e => { e.preventDefault(); executar(enviarComentario) }}>
                <input aria-label="Comentário" maxLength={500} value={comentario}
                    onChange={e => comentar(e.target.value)} />
                <button disabled={ocupado || !comentario.trim()}>Comentar</button>
            </form>
        </section>}
        {aviso && <p role="status">{aviso}</p>}
    </article>
}
