import { talk } from './talkApi'

export async function carregarPerfilAutora(id, pagina = 0) {
    // Projeção comunitária separada; nunca usa /perfil (perfil privado da sessão).
    const perfil = await talk('/comunidade/perfis/' + encodeURIComponent(id))
    const publicacoes = await talk('/comunidade/perfis/' + encodeURIComponent(id) + '/publicacoes?pagina=' + pagina)
    return { perfil, publicacoes }
}
