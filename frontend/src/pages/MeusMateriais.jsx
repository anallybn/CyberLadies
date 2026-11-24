import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { Archive, BookOpen, Clock3, Eye, FileText, FileVideo, Layers, Plus, Search, SearchX } from 'lucide-react'
import TopoLogado from '../components/TopoLogado'
import { useConta } from '../hooks/useConta'
import { consultarMeusMateriais, filtrarMateriais } from '../services/educadoraMateriaisService'
import { CATEGORIAS, NIVEIS, duracao } from '../services/academyService'
import './MeusMateriais.css'

const FILTROS = [['TODOS', 'Todos'], ['PUBLICADO', 'Publicados'], ['RASCUNHO', 'Rascunhos'], ['ARQUIVADO', 'Arquivados']]

export default function MeusMateriais() {
    const { conta, carregando, erro: erroConta, expirada, recarregar } = useConta()
    const [tentativa, setTentativa] = useState(0)
    const [estado, setEstado] = useState({})
    const [busca, setBusca] = useState('')
    const [status, setStatus] = useState('TODOS')
    const [trilha, setTrilha] = useState('')
    const educadora = conta?.perfil === 'EDUCADORA'
    const chave = conta?.id + ':' + tentativa
    useEffect(() => {
        if (!educadora) return
        const controle = new AbortController()
        consultarMeusMateriais(controle.signal).then((dados) => {
            if (!controle.signal.aborted) setEstado({ chave, dados })
        }).catch((erro) => {
            if (erro.name !== 'AbortError' && !controle.signal.aborted)
                setEstado({ chave, erro: erro.message, acesso: erro.status })
        })
        return () => controle.abort()
    }, [educadora, chave])
    const atual = educadora && estado.chave === chave
    const dados = atual ? estado.dados : null
    const erro = erroConta || (atual ? estado.erro : null)
    const acesso = atual ? estado.acesso : null
    if (expirada || acesso === 401 || (!sessionStorage.getItem('token') && !carregando))
        return <Navigate to="/login?perfil=educadora" replace />
    const negado = (conta && !educadora) || acesso === 403
    const carregandoMateriais = !negado && !erro && (carregando || !dados)
    const materiais = dados ? filtrarMateriais(dados.materiais, busca, status, trilha) : []
    const futuro = status === 'ARQUIVADO'
    const total = dados?.materiais.length
    function limpar() { setBusca(''); setStatus('TODOS'); setTrilha('') }

    return <div className="ed-dashboard mm-pagina">
        <TopoLogado ativa="materiais" />
        <main className="ed-conteudo mm-conteudo">
            <header className="mm-apresentacao">
                <h1>Meus Materiais</h1>
                <p>Gerencie todos os seus conteúdos publicados e rascunhos</p>
            </header>
            {negado ? <section className="mm-estado" role="alert"><FileText size={30} aria-hidden="true" /><h2>Acesso exclusivo da Educadora</h2><p>Entre com uma conta Educadora para consultar seus materiais.</p><Link to="/academy">Acessar CyberAcademy</Link></section> : <>
                <dl className="mm-indicadores">
                    <div><dt>Total de materiais</dt><dd>{total ?? '—'}</dd></div>
                    <div><dt>Publicados</dt><dd>{dados?.publicados ?? '—'}</dd></div>
                    <div><dt>Rascunhos</dt><dd>{dados?.rascunhos ?? '—'}</dd></div>
                </dl>
                <section className="mm-consulta" aria-label="Pesquisar e filtrar materiais">
                    <div className="mm-pesquisa"><Search size={18} aria-hidden="true" /><label className="mm-sr" htmlFor="mm-busca">Pesquisar materiais por título</label><input id="mm-busca" type="search" value={busca} maxLength={160} placeholder="Buscar material por título…" onChange={(evento) => setBusca(evento.target.value)} /></div>
                    <div className="mm-filtros"><div className="mm-status" role="group" aria-label="Status do material">{FILTROS.map(([valor, rotulo]) => <button type="button" key={valor} aria-pressed={status === valor} onClick={() => setStatus(valor)}>{rotulo}</button>)}</div>
                        {!!dados?.trilhas.length && <label className="mm-trilha" htmlFor="mm-trilha"><span>Trilha</span><select id="mm-trilha" value={trilha} onChange={(evento) => setTrilha(evento.target.value)}><option value="">Todas as minhas trilhas</option>{dados.trilhas.map((item) => <option key={item.id} value={item.id}>{item.titulo}</option>)}</select></label>}
                    </div>
                </section>
                {erro ? <section className="mm-estado" role="alert"><FileText size={30} aria-hidden="true" /><h2>Não foi possível carregar seus materiais</h2><p>{erro}</p><button type="button" onClick={() => erroConta ? recarregar() : setTentativa((valor) => valor + 1)}>Tentar novamente</button></section> : carregandoMateriais ? <section className="mm-estado" role="status" aria-busy="true"><span className="mm-carregando" aria-hidden="true" /><p>Carregando seus materiais…</p></section> : <>
                    {materiais.length > 0 ? <>
                        <div className="mm-resultado"><p role="status">{materiais.length} {materiais.length === 1 ? 'material encontrado' : 'materiais encontrados'}</p><small>Rascunhos por atualização; aulas por trilha e ordem.</small></div>
                        <section className="mm-grade" aria-label="Seus materiais">{materiais.map((material) => <article key={material.status + '-' + material.id} className="mm-material">
                            <div className="mm-miniatura"><span className="mm-publicado">{material.status === 'RASCUNHO' ? 'Rascunho' : 'Publicado'}</span><span className="mm-duracao"><Clock3 size={12} aria-hidden="true" />{material.duracaoMinutos == null ? 'A definir' : duracao(material.duracaoMinutos)}</span><FileVideo size={36} strokeWidth={1.25} aria-hidden="true" /></div>
                            <div className="mm-material-corpo"><span className="mm-categoria">{CATEGORIAS[material.categoria] || material.categoria || 'Categoria a definir'}</span><h2>{material.titulo}</h2><p className="mm-material-trilha"><Layers size={14} aria-hidden="true" />{material.trilha}</p><p className="mm-nivel"><BookOpen size={14} aria-hidden="true" />{NIVEIS[material.nivel] || material.nivel || 'Nível a definir'}</p><Link className="mm-visualizar" to={material.status === 'RASCUNHO' ? '/educadora/enviar-material?rascunho=' + material.id : '/academy/aulas/' + material.id}><Eye size={15} aria-hidden="true" />{material.status === 'RASCUNHO' ? 'Continuar editando' : 'Visualizar aula'}</Link></div>
                        </article>)}</section>
                    </> : <section className="mm-estado">
                        {futuro ? <><Archive size={32} aria-hidden="true" /><h2>Organize seus materiais em uma próxima etapa</h2><p>O arquivamento de materiais ainda não está disponível.</p><button type="button" onClick={limpar}>Ver todos os materiais</button></> :
                        busca.trim() || trilha ? <><SearchX size={32} aria-hidden="true" /><h2>Nenhum material encontrado</h2><p>Experimente outro título ou selecione uma trilha diferente.</p><button type="button" onClick={limpar}>Limpar filtros</button></> :
                        <><FileText size={32} aria-hidden="true" /><h2>{status === 'RASCUNHO' ? 'Nenhum rascunho salvo' : status === 'PUBLICADO' ? 'Nenhum material publicado' : 'Seu conhecimento vai ganhar espaço aqui'}</h2><p>{status === 'PUBLICADO' ? 'Seus rascunhos continuam privados. A publicação permanece indisponível nesta etapa.' : 'Salve um rascunho em Novo Material para continuar sua preparação depois.'}</p></>}
                    </section>}
                    <p className="mm-nota">Rascunhos são privados e não aparecem no CyberAcademy. A publicação e o arquivamento continuam indisponíveis.</p>
                </>}
                <footer className="mm-acoes"><Link to="/educadora/enviar-material"><Plus size={17} aria-hidden="true" />Novo Material</Link></footer>
            </>}
        </main>
    </div>
}
