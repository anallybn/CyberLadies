import { useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { UserRound } from 'lucide-react'
import AreaConta from '../components/AreaConta'
import PostTalk from '../components/PostTalk'
import { carregarPerfilAutora } from '../services/perfilAutoraService'
import './Perfil.css'

function Conteudo({ id, pagina }) {
    const [dados, setDados] = useState(null)
    const [erro, setErro] = useState(null)
    const [tentativa, setTentativa] = useState(0)
    const titulo = useRef(null)
    const carregado = Boolean(dados || erro)
    useEffect(() => {
        let ativo = true
        carregarPerfilAutora(id, pagina).then(resultado => {
            if (ativo) setDados(resultado)
        }).catch(falha => { if (ativo) setErro(falha) })
        return () => { ativo = false }
    }, [id, pagina, tentativa])
    useEffect(() => { if (carregado) titulo.current?.focus() }, [carregado])
    function recarregar() { setDados(null); setErro(null); setTentativa(v => v + 1) }
    const listagem = dados?.publicacoes
    return <AreaConta titulo="Perfil no CyberTalk" ativa="comunidade">
        <Link to="/comunidade">← Voltar ao CyberTalk</Link>
        <div className="pf cm-feed">
            {erro ? <section className="pf-cartao">
                <h2 ref={titulo} tabIndex={-1}>{erro.status === 404 ? 'Perfil não encontrado' : 'Não foi possível abrir o perfil'}</h2>
                <p role="alert">{erro.message}</p>
                {erro.status === 403 ? <Link to="/comunidade">Participar da comunidade</Link> :
                    erro.status !== 404 && <button className="pf-secundario" onClick={recarregar}>Tentar novamente</button>}
            </section> : !dados ? <p role="status">Carregando perfil…</p> : <>
                <section className="pf-cartao" aria-label="Identidade da autora">
                    <UserRound size={40} aria-hidden="true" />
                    <h2 ref={titulo} tabIndex={-1}>{dados.perfil.nome}</h2>
                </section>
                <section className="cm-feed" aria-labelledby="publicacoes-autora">
                    <h2 id="publicacoes-autora">Publicações</h2>
                    {listagem.itens.length === 0 ? <p className="pf-vazio">Nenhuma publicação nesta página.</p> :
                        listagem.itens.map(post => <PostTalk key={post.id} post={post}
                            atualizar={novo => setDados(atual => ({ ...atual, publicacoes: { ...atual.publicacoes,
                                itens: atual.publicacoes.itens.map(item => item.id === novo.id ? novo : item) } }))}
                            remover={recarregar} />)}
                    <nav className="pf-acoes" aria-label="Páginas de publicações">
                        {pagina > 0 && <Link className="pf-secundario" to={'?pagina=' + (pagina - 1)}>Anterior</Link>}
                        <span aria-live="polite">Página {pagina + 1}</span>
                        {(pagina + 1) * listagem.tamanho < listagem.total &&
                            <Link className="pf-secundario" to={'?pagina=' + (pagina + 1)}>Próxima</Link>}
                    </nav>
                </section>
            </>}
        </div>
    </AreaConta>
}

export default function PerfilAutora() {
    const { id } = useParams()
    const [params] = useSearchParams()
    const valor = params.get('pagina') || '0'
    const pagina = /^\d{1,9}$/.test(valor) ? Number(valor) : 0
    return <Conteudo key={id + ':' + pagina} id={id} pagina={pagina} />
}
