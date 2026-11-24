import { API_BASE_URL as API } from '../services/apiUrl.js'
import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { BookOpen, Check, Clock3, Eye, FileText, Info, Layers, LockKeyhole, Play, Save, Upload } from 'lucide-react'
import TopoLogado from '../components/TopoLogado'
import TituloEducadora from '../components/TituloEducadora'
import { academy, CATEGORIAS, NIVEIS } from '../services/academyService'
import mascoteEducadora from '../assets/gatinho-educadora-minimalista.png'
import { useRascunho } from '../hooks/useRascunho'
import { useAlteracoesPendentes } from '../hooks/useAlteracoesPendentes'
import './EnviarMaterial.css'


export default function EnviarMaterial() {
    const [estado, setEstado] = useState({ carregando: true })
    const [tentativa, setTentativa] = useState(0)
    const edicao = useRascunho()
    const { campos, atualizar } = edicao
    const confirmarSaida = useAlteracoesPendentes(edicao.sujo)
    const [detalhe, setDetalhe] = useState(null)
    useEffect(() => {
        const controle = new AbortController()
        async function carregar() {
            try {
                const resposta = await fetch(API + '/educadora/dashboard', {
                    headers: { Authorization: 'Bearer ' + (sessionStorage.getItem('token') || '') },
                    signal: controle.signal, cache: 'no-store',
                })
                if ([401, 403].includes(resposta.status)) { setEstado({ acesso: resposta.status }); return }
                if (!resposta.ok) throw new Error('Não foi possível consultar sua conta e suas trilhas.')
                const dados = await resposta.json()
                if (!controle.signal.aborted) setEstado({ dados })
            } catch (erro) {
                if (erro.name !== 'AbortError') setEstado({ erro: erro.message })
            }
        }
        carregar()
        return () => controle.abort()
    }, [tentativa])
    useEffect(() => {
        if (!campos.trilha) return
        const controle = new AbortController()
        const id = campos.trilha
        academy('/trilhas/' + id, { signal: controle.signal }).then((dados) => {
            if (!controle.signal.aborted) setDetalhe({ id, dados })
        }).catch((erro) => {
            if (erro.name !== 'AbortError') setDetalhe({ id, erro: 'Não foi possível consultar a categoria e o nível desta trilha.' })
        })
        return () => controle.abort()
    }, [campos.trilha])

    if (estado.acesso === 401) return <Navigate to="/login?perfil=educadora" replace />
    const dados = estado.dados
    const trilhas = dados?.trilhas || []
    const trilha = trilhas.find((item) => String(item.id) === campos.trilha)
    const metadados = detalhe?.id === campos.trilha ? detalhe : null
    const tags = [...new Set(campos.tags.split(',').map((tag) => tag.trim()).filter(Boolean))]
    const videoValido = !campos.video || (campos.provedor === 'YOUTUBE' ? /^[A-Za-z0-9_-]{11}$/ : /^[1-9][0-9]{0,11}$/).test(campos.video)
    const categoria = CATEGORIAS[metadados?.dados?.categoria]
    const nivel = NIVEIS[metadados?.dados?.nivel]
    const estadoTrilha = !campos.trilha ? 'Selecione uma trilha' : metadados?.erro ? 'Indisponível' : 'Carregando…'

    return <div className="ed-dashboard em-pagina">
        <TopoLogado ativa="enviar" confirmarSaida={confirmarSaida} />
        <main className="ed-conteudo em-conteudo">
            {estado.carregando && <p role="status" className="em-aviso">Carregando sua conta…</p>}
            {estado.acesso === 403 && <section className="em-card ed-estado" role="alert"><h1>Acesso exclusivo da Educadora</h1><p>Esta conta não possui acesso à área da Educadora.</p><Link to="/academy">Acessar CyberAcademy</Link></section>}
            {estado.erro && <div className="em-aviso" role="alert"><p>{estado.erro}</p><button type="button" onClick={() => { setEstado({ carregando: true }); setTentativa((valor) => valor + 1) }}>Tentar novamente</button></div>}
            {dados && <>
                <section className="em-apresentacao">
                    <div>
                        <Link className="em-voltar" to="/educadora/dashboard">Painel da educadora <span aria-hidden="true">/</span></Link>
                        <TituloEducadora icone={BookOpen} inicio="Enviar" destaque="Material"
                            subtitulo="Transforme seu conhecimento em novas possibilidades." />
                        <span className="em-etiqueta"><FileText size={13} aria-hidden="true" /> Seu espaço de criação</span>
                    </div>

                </section>
                <div className="em-aviso em-aprovacao" role="status">
                    <Info size={18} aria-hidden="true" />
                    <div><strong>{dados.statusEducadora === 'PENDENTE' ? 'Sua conta está pendente de aprovação' : dados.statusEducadora === 'APROVADA' ? 'Sua conta está aprovada' : 'Sua conta ainda não está aprovada para publicar'}</strong>
                        <p>Você pode salvar e editar seus rascunhos. A publicação de materiais continua indisponível nesta etapa.</p>
                    </div>
                </div>
                {edicao.carregando && <p role="status" className="em-aviso">{edicao.erroCarga || 'Carregando rascunho…'}{edicao.erroCarga && <button type="button" onClick={edicao.recarregar}>Tentar novamente</button>}</p>}
                <div className="em-layout">
                    <form className="em-formulario" inert={edicao.carregando || undefined} onSubmit={(evento) => { evento.preventDefault(); edicao.salvar() }}>
                        <section className="em-card em-card-informacoes" aria-labelledby="em-informacoes">
                            <header className="em-card-titulo"><img src={mascoteEducadora} alt="" aria-hidden="true" className="em-mascote-card" /><span><FileText size={18} /></span><div><h2 id="em-informacoes">Informações do material</h2><p>Comece pelo que você quer ensinar.</p></div></header>
                            <div className="em-campos">
                                <label htmlFor="em-titulo">Título do material<input id="em-titulo" name="titulo" aria-required="true" aria-describedby="em-salvar-feedback" value={campos.titulo} onChange={atualizar} maxLength={160} placeholder="Dê um título ao seu conteúdo" /></label>
                                <label htmlFor="em-descricao">Descrição<textarea id="em-descricao" name="descricao" value={campos.descricao} onChange={atualizar} maxLength={5000} rows={4} placeholder="Conte o que a aluna poderá aprender" /><small>{campos.descricao.length}/5000 caracteres</small></label>
                                <label htmlFor="em-trilha">Trilha de aprendizagem<select id="em-trilha" name="trilha" value={campos.trilha} onChange={atualizar} disabled={!trilhas.length} aria-describedby="em-ajuda-trilha"><option value="">{trilhas.length ? 'Selecione uma das suas trilhas' : 'Nenhuma trilha vinculada à sua conta'}</option>{trilhas.map((item) => <option key={item.id} value={item.id}>{item.titulo}</option>)}</select></label>
                                <p id="em-ajuda-trilha" className="em-ajuda">{trilhas.length ? 'Somente suas trilhas são exibidas. A disponibilidade para publicação será validada antes de publicar.' : 'Quando uma trilha estiver vinculada à sua conta, ela aparecerá aqui. Você pode explorar os demais campos enquanto isso.'}</p>
                                <div className="em-dupla">
                                    <label htmlFor="em-categoria">Categoria / área<input id="em-categoria" readOnly value={categoria || estadoTrilha} /></label>
                                    <label htmlFor="em-nivel">Nível<input id="em-nivel" readOnly value={nivel || estadoTrilha} /></label>
                                </div>
                                {metadados?.erro && <p className="em-erro" role="alert">{metadados.erro}</p>}
                                <p className="em-ajuda">Categoria e nível acompanham a trilha selecionada.</p>
                                <div className="em-dupla">
                                    <label htmlFor="em-duracao">Duração (minutos)<input id="em-duracao" name="duracao" type="number" min="1" step="1" value={campos.duracao} onChange={atualizar} placeholder="Informe a duração" /></label>
                                    <label htmlFor="em-tipo">Tipo de material<select id="em-tipo" defaultValue="VIDEO"><option value="VIDEO">Videoaula</option><option disabled>PDF · Em breve</option><option disabled>Envio de arquivo MP4 · Em breve</option></select></label>
                                </div>
                                <label htmlFor="em-tags">Tags<input id="em-tags" name="tags" value={campos.tags} onChange={atualizar} maxLength={250} placeholder="Separe as palavras-chave por vírgulas" /><small>Adicione palavras-chave que descrevam seu material.</small></label>
                            </div>
                        </section>
                        <section className="em-card" aria-labelledby="em-video">
                            <header className="em-card-titulo"><span><Play size={18} /></span><div><h2 id="em-video">Vídeo e autorização</h2><p>Um conteúdo seu ou que você tenha autorização para compartilhar.</p></div></header>
                            <div className="em-campos">
                                <label htmlFor="em-provedor">Plataforma do vídeo<select id="em-provedor" name="provedor" value={campos.provedor} onChange={atualizar}><option value="YOUTUBE">YouTube</option><option value="VIMEO">Vimeo</option></select></label>
                                <label htmlFor="em-video-id">Identificador do vídeo<input id="em-video-id" name="video" value={campos.video} onChange={atualizar} maxLength={20} placeholder="Informe apenas o identificador" aria-invalid={!videoValido} aria-describedby="em-ajuda-video" /></label>
                                <p id="em-ajuda-video" className={videoValido ? 'em-ajuda' : 'em-erro'}>{videoValido ? campos.provedor === 'YOUTUBE' ? 'YouTube: identificador com 11 caracteres. Não cole links ou código HTML.' : 'Vimeo: identificador numérico de até 12 dígitos. Não cole links ou código HTML.' : 'Confira o identificador para a plataforma selecionada.'}</p>
                                <label className="em-checkbox" htmlFor="em-autorizado"><input id="em-autorizado" name="autorizado" type="checkbox" checked={campos.autorizado} onChange={atualizar} /><span>Confirmo que sou autora ou possuo autorização para utilizar e compartilhar este vídeo.</span></label>
                                <p className="em-ajuda">A confirmação faz parte da preparação e não publica o vídeo. Não há envio de arquivos nesta etapa.</p>
                            </div>
                        </section>
                        <div className="em-acoes">
                            {edicao.mensagem && <div id="em-salvar-feedback" className="em-aviso" role={edicao.mensagem.erro ? 'alert' : 'status'}><p>{edicao.mensagem.texto}</p>{edicao.mensagem.conflito && <button type="button" onClick={edicao.recarregar}>Recarregar versão salva</button>}</div>}
                            <p id="em-acoes-ajuda"><LockKeyhole size={15} aria-hidden="true" /> Salvar rascunho está disponível. Publicar material permanece indisponível.</p>
                            <div><button type="button" disabled={!edicao.pronto || edicao.salvando} aria-busy={edicao.salvando} onClick={edicao.salvar} aria-describedby="em-acoes-ajuda"><Save size={16} />{edicao.salvando ? 'Salvando…' : 'Salvar rascunho'}</button><button type="button" className="em-publicar" disabled aria-describedby="em-acoes-ajuda"><Upload size={16} />Publicar material</button></div>
                        </div>
                    </form>
                    <aside className="em-previa" aria-labelledby="em-previa-titulo">
                        <div className="em-previa-cabecalho"><Eye size={17} aria-hidden="true" /><h2 id="em-previa-titulo">Prévia do material</h2><span>Não publicado</span></div>
                        <article className="em-card em-previa-card">
                            <div className="em-previa-video"><span><Play size={27} aria-hidden="true" /></span><p>Seu vídeo aparecerá aqui</p><small>A reprodução estará disponível em uma próxima etapa.</small></div>
                            <div className="em-previa-corpo"><span className="em-previa-tipo">VIDEOAULA</span><h3>{campos.titulo.trim() || 'Título do seu material'}</h3><p className="em-previa-descricao">{campos.descricao.trim() || 'A descrição do seu conteúdo aparecerá neste espaço.'}</p>
                                <div className="em-previa-meta"><span><Layers size={14} />{categoria || 'Categoria da trilha'}</span><span><BookOpen size={14} />{nivel || 'Nível da trilha'}</span><span><Clock3 size={14} />{/^[1-9][0-9]*$/.test(campos.duracao) ? campos.duracao + ' min' : 'Duração a informar'}</span></div>
                                <p className="em-previa-trilha">{trilha?.titulo || 'Trilha ainda não selecionada'}</p>
                                {tags.length > 0 && <div className="em-tags">{tags.map((tag) => <span key={tag}>{tag}</span>)}</div>}
                            </div>
                        </article>
                        <p className="em-previa-nota"><Check size={15} aria-hidden="true" /> {edicao.sujo ? 'Há alterações ainda não salvas. Nenhum material foi publicado.' : edicao.id ? 'Rascunho salvo. Nenhum material foi publicado.' : 'A prévia acompanha seus campos. Salve para preservar este material.'}</p>
                    </aside>
                </div>
            </>}
        </main>
    </div>
}
