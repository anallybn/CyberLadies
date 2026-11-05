import { useEffect, useState } from 'react'
import { talk } from '../services/talkApi'

export default function AnexoTalk({ post }) {
    const [url, setUrl] = useState('')
    const [erro, setErro] = useState('')
    const [abrir, setAbrir] = useState(false)
    useEffect(() => {
        if (!abrir) return
        let ativo = true
        let objeto
        talk('/posts/' + post.id + '/anexo', 'GET', undefined, true)
            .then(blob => {
                if (ativo) {
                    objeto = URL.createObjectURL(blob)
                    setUrl(objeto)
                }
            })
            .catch(e => { if (ativo) { setErro(e.message); setAbrir(false) } })
        return () => { ativo = false; if (objeto) URL.revokeObjectURL(objeto) }
    }, [abrir, post.id])
    const foto = ['image/jpeg', 'image/png'].includes(post.anexoTipo)
    return <div className="cm-anexo">
        {url ? <>
            {foto && <img src={url} alt={post.anexoNome} />}
            <a href={url} download={post.anexoNome}>Baixar {post.anexoNome}</a>
        </> : <button type="button" disabled={abrir} onClick={() => { setErro(''); setAbrir(true) }}>
            {abrir ? 'Carregando…' : (foto ? 'Ver foto' : 'Abrir anexo')} · {post.anexoNome}
        </button>}
        {erro && <p role="alert">{erro}</p>}
    </div>
}
