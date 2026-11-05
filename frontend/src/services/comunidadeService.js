import { talk } from './talkApi'

export async function carregarComunidade(postId) {
    const usuaria = await talk('/usuaria')
    if (!usuaria.membro) return { usuaria, posts: [] }
    const posts = postId ? [await talk('/posts/' + encodeURIComponent(postId))] : await talk('/posts')
    return { usuaria, posts }
}

export function publicarPost(texto, arquivo) {
    if (!arquivo) return talk('/posts', 'POST', { texto })
    const dados = new FormData()
    dados.set('texto', texto)
    dados.set('arquivo', arquivo)
    return talk('/posts', 'POST', dados)
}
