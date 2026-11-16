import { useConta } from '../hooks/useConta'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { BriefcaseBusiness, Search, MapPin, ArrowRight } from 'lucide-react'
import TopoLogado from '../components/TopoLogado'
import Circuitos from '../components/Circuitos'
import mascote from '../assets/gatinho-cybervagas.png'
import { listarVagas, buscarVaga, minhasCandidaturas, candidatar } from '../services/vagasService'
import './Vagas.css'

const INICIAIS = { busca: '', nivel: [], modalidade: [], salarioMinimo: '', paraMaes: false, pcd: false, semFormacao: false }
const NIVEIS = { ESTAGIO: 'Estágio', JUNIOR: 'Júnior', PLENO: 'Pleno', SENIOR: 'Sênior' }
const MODALIDADES = { REMOTO: 'Remoto', HIBRIDO: 'Híbrido', PRESENCIAL: 'Presencial' }
const moeda = (valor) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(valor)

function salario(vaga) {
    if (vaga.salarioMinimo != null && vaga.salarioMaximo != null) return moeda(vaga.salarioMinimo) + ' – ' + moeda(vaga.salarioMaximo)
    if (vaga.salarioMinimo != null) return 'A partir de ' + moeda(vaga.salarioMinimo)
    if (vaga.salarioMaximo != null) return 'Até ' + moeda(vaga.salarioMaximo)
    return 'Salário não informado'
}

export default function Vagas() {
    const { conta, carregando: consultandoConta, erro: erroConta } = useConta()
    const podeCandidatar = conta?.perfil === 'CYBERLADIE'
    const contaRestrita = !!conta && !podeCandidatar
    const [filtros, setFiltros] = useState(INICIAIS)
    const [resultado, setResultado] = useState(null)
    const [carregando, setCarregando] = useState(true)
    const [erro, setErro] = useState('')
    const [tentativa, setTentativa] = useState(0)
    const [params] = useSearchParams()
    const vagaId = params.get('vaga')
    const [detalhe, setDetalhe] = useState({ carregando: false, vaga: null, erro: '' })
    const [candidaturas, setCandidaturas] = useState([])
    const [enviando, setEnviando] = useState(null)
    const [feedback, setFeedback] = useState(null)
    const bloqueioEnvio = useRef(false)
    const navigate = useNavigate()

    useEffect(() => {
        const controller = new AbortController()
        const timer = setTimeout(() => {
            setCarregando(true)
            setErro('')
            listarVagas(filtros, controller.signal)
                .then(setResultado)
                .catch((falha) => { if (falha.name !== 'AbortError') setErro(falha.message) })
                .finally(() => { if (!controller.signal.aborted) setCarregando(false) })
        }, 250)
        return () => { clearTimeout(timer); controller.abort() }
    }, [filtros, tentativa])

    useEffect(() => {
        if (!podeCandidatar) return
        const controller = new AbortController()
        minhasCandidaturas(controller.signal)
            .then((lista) => setCandidaturas(lista.map((c) => c.vagaId)))
            .catch((falha) => {
                if (falha.name !== 'AbortError') setFeedback({ erro: true, texto: falha.message })
            })
        return () => controller.abort()
    }, [podeCandidatar, conta?.id])
    useEffect(() => {
        if (!vagaId) return
        const controller = new AbortController()
        const timer = setTimeout(() => {
            setDetalhe({ carregando: true, vaga: null, erro: '' })
            if (!/^[1-9]\d*$/.test(vagaId)) {
                setDetalhe({ carregando: false, vaga: null, erro: 'Identificador de vaga inválido.' })
                return
            }
            buscarVaga(vagaId, controller.signal)
                .then((vaga) => setDetalhe({ carregando: false, vaga, erro: '' }))
                .catch((falha) => {
                    if (falha.name !== 'AbortError') setDetalhe({ carregando: false, vaga: null, erro: falha.message })
                })
        }, 0)
        return () => { clearTimeout(timer); controller.abort() }
    }, [vagaId])

    function mudar(chave, valor) {
        setCarregando(true)
        setFiltros((atuais) => ({ ...atuais, [chave]: valor }))
    }
    function marcar(grupo, valor) {
        mudar(grupo, filtros[grupo].includes(valor) ? filtros[grupo].filter((item) => item !== valor) : [...filtros[grupo], valor])
    }
    function entrar(id) {
        navigate('/login?perfil=usuaria&retorno=' + encodeURIComponent('/vagas?vaga=' + id))
    }
    async function enviar(id) {
        if (bloqueioEnvio.current || candidaturas.includes(id)) return
        if (!sessionStorage.getItem('token')) { entrar(id); return }
        if (!podeCandidatar || consultandoConta || erroConta) return
        bloqueioEnvio.current = true
        setEnviando(id)
        setFeedback(null)
        try {
            await candidatar(id)
            setCandidaturas((atuais) => [...atuais, id])
            setFeedback({ erro: false, texto: 'Candidatura enviada! Sua candidatura está pendente de análise.' })
        } catch (falha) {
            if (falha.status === 401) { entrar(id); return }
            if (falha.status === 409) setCandidaturas((atuais) => [...atuais, id])
            setFeedback({ erro: true, texto: falha.message })
        } finally {
            bloqueioEnvio.current = false
            setEnviando(null)
        }
    }
    function botaoCandidatura(vaga) {
        const enviada = candidaturas.includes(vaga.id)
        return <button type="button" className="cv-botao" disabled={enviando !== null || enviada || contaRestrita || consultandoConta || !!erroConta}
            aria-busy={enviando === vaga.id} onClick={() => enviar(vaga.id)}>
            {consultandoConta ? 'Confirmando conta…' : erroConta ? 'Conta não confirmada' : contaRestrita ? 'Exclusivo para CyberLadie' : enviando === vaga.id ? 'Enviando…' : enviada ? 'Candidatura enviada' : 'Candidatar-se'}
            {!enviada && enviando !== vaga.id && <ArrowRight size={16} aria-hidden="true" />}
        </button>
    }
    function cartao(vaga, completo = false) {
        return <article className="cv-cartao" key={vaga.id}>
            <div className="cv-cartao-topo">
                <span className="cv-empresa-icone" aria-hidden="true"><BriefcaseBusiness size={22} /></span>
                <div className="cv-identificacao">
                    <h2>{completo ? vaga.titulo : <Link to={'/vagas?vaga=' + vaga.id}>{vaga.titulo}</Link>}</h2>
                    <p>{vaga.empresa}</p>
                    <p className="cv-local"><MapPin size={14} aria-hidden="true" />{[vaga.localizacao, MODALIDADES[vaga.modalidade]].filter(Boolean).join(' · ') || 'Localização não informada'}</p>
                </div>
                <strong className="cv-salario">{salario(vaga)}</strong>
            </div>
            <div className="cv-tags">
                {[NIVEIS[vaga.nivel], ...(vaga.tecnologias || '').split(',').map((t) => t.trim()),
                    vaga.paraMaes && 'Para mães', vaga.pcd && 'PCD', vaga.semFormacao && 'Sem prévia formação']
                    .filter(Boolean).map((tag, indice) => <span key={indice}>{tag}</span>)}
            </div>
            {completo && <div className="cv-descricao">
                <h3>Sobre a oportunidade</h3><p>{vaga.descricao}</p>
                {vaga.requisitos && <><h3>Requisitos</h3><p>{vaga.requisitos}</p></>}
            </div>}
            <div className="cv-cartao-rodape">
                <small>Publicada em <time dateTime={vaga.publicadoEm}>{new Date(vaga.publicadoEm).toLocaleDateString('pt-BR')}</time></small>
                <div className="cv-acoes">{!completo && <Link to={'/vagas?vaga=' + vaga.id}>Ver detalhes</Link>}{botaoCandidatura(vaga)}</div>
            </div>
        </article>
    }
    return <div className="cv">
        <TopoLogado ativa="vagas" /><Circuitos />
        <main className="cv-conteudo">
            <section className="cv-apresentacao" aria-labelledby="cv-titulo">
                <div>
                    <span className="cv-etiqueta">CYBERVAGAS · SEU PRÓXIMO PASSO</span>
                    <h1 id="cv-titulo">Encontre sua vaga no mercado tecnológico</h1>
                    <p>Seu talento tem lugar na tecnologia. Explore oportunidades inclusivas para mulheres, inclusive para quem está em transição de carreira.</p>
                    <div className="cv-indicadores">
                        <div><strong>{resultado ? resultado.vagasDisponiveis : '—'}</strong><span>Vagas disponíveis</span></div>
                        <div><strong>{resultado ? resultado.empresas : '—'}</strong><span>Empresas com vagas</span></div>
                    </div>
                </div>
                <img src={mascote} alt="" className="cv-mascote" />
            </section>
            {feedback && <div className={'cv-mensagem' + (feedback.erro ? ' cv-erro' : '')} role={feedback.erro ? 'alert' : 'status'}>
                {feedback.texto}<button type="button" onClick={() => setFeedback(null)} aria-label="Fechar mensagem">×</button>
            </div>}
            {vagaId ? <section className="cv-detalhe" aria-label="Detalhes da vaga">
                <Link to="/vagas" className="cv-voltar">← Voltar às vagas</Link>
                {detalhe.carregando && <p role="status">Carregando detalhes…</p>}
                {detalhe.erro && <p role="alert" className="cv-estado">{detalhe.erro}</p>}
                {detalhe.vaga && cartao(detalhe.vaga, true)}
            </section> : <>
                <div className="cv-pesquisa">
                    <Search size={20} aria-hidden="true" />
                    <label htmlFor="cv-busca" className="cv-sr">Buscar por título, empresa ou palavra-chave</label>
                    <input id="cv-busca" type="search" maxLength={150} placeholder="Título, empresa ou palavra-chave"
                        value={filtros.busca} onChange={(e) => mudar('busca', e.target.value)} />
                </div>
                <div className="cv-grade">
                    <aside className="cv-filtros">
                        <details open>
                            <summary>Filtros <span>Refine sua busca</span></summary>
                            <div className="cv-filtros-conteudo">
                                <fieldset><legend>Nível</legend>
                                    {Object.entries(NIVEIS).map(([valor, rotulo]) => <label key={valor}>
                                        <input type="checkbox" checked={filtros.nivel.includes(valor)} onChange={() => marcar('nivel', valor)} />{rotulo}
                                    </label>)}
                                </fieldset>
                                <fieldset><legend>Modalidade</legend>
                                    {Object.entries(MODALIDADES).map(([valor, rotulo]) => <label key={valor}>
                                        <input type="checkbox" checked={filtros.modalidade.includes(valor)} onChange={() => marcar('modalidade', valor)} />{rotulo}
                                    </label>)}
                                </fieldset>
                                <fieldset><legend>Faixa salarial</legend>
                                    <label htmlFor="cv-salario">Piso desejado</label>
                                    <select id="cv-salario" value={filtros.salarioMinimo} onChange={(e) => mudar('salarioMinimo', e.target.value)}>
                                        <option value="">Qualquer salário</option>
                                        {[1000, 2000, 3000, 5000, 8000, 10000].map((valor) => <option value={valor} key={valor}>{moeda(valor)}</option>)}
                                    </select>
                                    <small>Faixas que alcançam o piso. Salários não informados ficam de fora quando filtrados.</small>
                                </fieldset>
                                <fieldset><legend>Inclusão</legend>
                                    {[['paraMaes', 'Vagas para mães'], ['pcd', 'PCD'], ['semFormacao', 'Sem prévia formação']].map(([chave, rotulo]) =>
                                        <label key={chave}><input type="checkbox" checked={filtros[chave]} onChange={(e) => mudar(chave, e.target.checked)} />{rotulo}</label>)}
                                </fieldset>
                                <button className="cv-limpar" type="button" onClick={() => { setCarregando(true); setFiltros({ ...INICIAIS }) }}>Limpar filtros</button>
                            </div>
                        </details>
                    </aside>
                    <section className="cv-resultados" aria-label="Vagas encontradas" aria-busy={carregando}>
                        <p className="cv-contador" role="status">{carregando ? 'Buscando oportunidades…' : erro ? 'Busca indisponível' :
                            (resultado?.totalResultados ?? 0) + ((resultado?.totalResultados ?? 0) === 1 ? ' vaga encontrada' : ' vagas encontradas')}</p>
                        {erro ? <div className="cv-estado"><p role="alert">{erro}</p>
                            <button className="cv-botao" onClick={() => { setCarregando(true); setTentativa((t) => t + 1) }}>Tentar novamente</button></div>
                            : carregando ? <div className="cv-estado" role="status"><span className="cv-spinner" />Carregando vagas…</div>
                            : resultado?.vagas.length ? resultado.vagas.map((vaga) => cartao(vaga))
                            : <div className="cv-estado"><BriefcaseBusiness size={36} aria-hidden="true" />
                                <h2>{resultado?.vagasDisponiveis ? 'Nenhuma vaga corresponde aos filtros' : 'Nenhuma vaga disponível no momento'}</h2>
                                <p>{resultado?.vagasDisponiveis ? 'Experimente outros critérios ou limpe os filtros.' : 'Novas oportunidades aparecerão aqui assim que forem publicadas.'}</p></div>}
                    </section>
                </div>
            </>}
        </main>
    </div>
}