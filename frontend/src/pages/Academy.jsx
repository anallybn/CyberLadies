import { useConta } from '../hooks/useConta'
import AcademyMentoras from '../components/AcademyMentoras'
import AcademyIniciar from '../components/AcademyIniciar'
import { useAcademy } from '../hooks/useAcademy'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Search, PlayCircle, GraduationCap } from 'lucide-react'
import { AcademyShell, AcademyEstado, AcademyProgresso } from '../components/AcademyUI'
import { CATEGORIAS, NIVEIS, duracao } from '../services/academyService'
import mascote from '../assets/gatinho-academy.png'

export default function Academy() {
    const { conta } = useConta()
    const estudante = ['CYBERLADIE', 'EDUCADORA'].includes(conta?.perfil)
    const [busca, setBusca] = useState('')
    const [termo, setTermo] = useState('')
    const [categoria, setCategoria] = useState('')
    const [nivel, setNivel] = useState('')
    useEffect(() => { const timer = setTimeout(() => setTermo(busca.trim()), 250); return () => clearTimeout(timer) }, [busca])
    const params = new URLSearchParams({ busca: termo, categoria, nivel })
    const estado = useAcademy('/catalogo?' + params)
    const dados = estado.dados
    const meus = useAcademy('/me/progresso', true, estudante)
    return <AcademyShell>
        <section className="ac-hero">
            <div><span className="ac-etiqueta">CYBERACADEMY · APRENDA NO SEU RITMO</span>
                <h1>Aprenda, evolua e conquiste seu espaço no <em>mundo tecnológico</em></h1>
                <p>Trilhas formativas, videoaulas e suporte contínuo, tudo pensado para mulheres que desejam ingressar, desenvolver ou se recolocar na área da tecnologia.</p>
                <dl className="ac-indicadores">{[['trilhas', 'Trilhas formativas'], ['videoaulas', 'Videoaulas'], ['areas', 'Áreas de estudo'], ['percentualGratuito', 'Gratuito']].map(([chave, rotulo]) =>
                    <div key={chave}><dt>{rotulo}</dt><dd>{dados?.indicadores[chave] == null ? '—' : dados.indicadores[chave] + (chave === 'percentualGratuito' ? '%' : '')}</dd></div>)}</dl>
            </div><img className="ac-mascote" src={mascote} alt="" />
        </section>
        <div className="ac-atalhos"><Link to="/academy/mentorias">Mentorias e meus agendamentos</Link></div>
        <div className="ac-pesquisa"><Search size={20} aria-hidden="true" /><label className="ac-sr" htmlFor="ac-busca">Pesquisar trilhas e aulas</label>
            <input id="ac-busca" type="search" maxLength={150} placeholder="Pesquise por nome, descrição ou assunto" value={busca} onChange={(e) => setBusca(e.target.value)} /></div>
        <div className="ac-filtros" aria-label="Filtros do catálogo">
            <div className="ac-categorias">{[['', 'Todas'], ...Object.entries(CATEGORIAS)].map(([valor, rotulo]) =>
                <button key={valor} type="button" aria-pressed={categoria === valor} onClick={() => setCategoria(valor)}>{rotulo}</button>)}</div>
            <label>Nível<select value={nivel} onChange={(e) => setNivel(e.target.value)}><option value="">Todos os níveis</option>
                {Object.entries(NIVEIS).map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}</select></label>
            <button className="ac-secundario" onClick={() => { setBusca(''); setTermo(''); setCategoria(''); setNivel('') }}>Limpar filtros</button>
        </div>
        {estado.carregando || estado.erro ? <AcademyEstado {...estado} /> : <>
            <section aria-labelledby="ac-trilhas"><h2 id="ac-trilhas">Trilhas em destaque</h2>
                <p className="ac-ajuda" role="status">{dados.trilhas.length} {dados.trilhas.length === 1 ? 'trilha encontrada' : 'trilhas encontradas'}</p>
                {!dados.trilhas.length ? <p className="ac-vazio">Nenhuma trilha disponível para esta busca.</p> :
                    <div className="ac-grade">{dados.trilhas.map((trilha) => <article className="ac-cartao ac-trilha" key={trilha.id}>
                        <span className="ac-tag">{NIVEIS[trilha.nivel]}</span><h3>{trilha.titulo}</h3><p>{trilha.descricao}</p>
                        <small>{CATEGORIAS[trilha.categoria]} · Versão {trilha.versao}</small><p className="ac-ajuda">{trilha.totalAulas} aulas · {duracao(trilha.cargaMinutos)}</p>
                                                {meus.dados?.some((p) => p.trilhaId === trilha.id) ?
                            <AcademyProgresso percentual={meus.dados.find((p) => p.trilhaId === trilha.id).percentual} /> : <p className="ac-ajuda">Inscreva-se para acompanhar seu progresso.</p>}
                        <AcademyIniciar trilha={trilha} inscrita={meus.dados?.some((p) => p.trilhaId === trilha.id)} />
                        <Link to={'/academy/trilhas/' + trilha.id}>Ver detalhes</Link>
                    </article>)}</div>}
            </section>
            <section aria-labelledby="ac-aulas"><h2 id="ac-aulas">Videoaulas recentes</h2>
                {!dados.aulas.length ? <p className="ac-vazio">Nenhuma videoaula disponível para esta busca.</p> :
                    <div className="ac-grade">{dados.aulas.map((aula) => <article className="ac-cartao ac-aula" key={aula.id}>
                        <div className="ac-miniatura"><span className="ac-tag">{duracao(aula.duracaoMinutos)}</span><PlayCircle size={42} aria-hidden="true" /></div>
                        <small>{CATEGORIAS[aula.categoria]} · {NIVEIS[aula.nivel]}</small><h3>{aula.titulo}</h3>
                        <Link className="ac-link" to={'/academy/aulas/' + aula.id}>Assistir aula →</Link>
                    </article>)}</div>}
            </section>
        </>}
        {meus.erro && <AcademyEstado erro={meus.erro} repetir={meus.repetir} />}
        <section aria-labelledby="ac-mentorias"><h2 id="ac-mentorias"><GraduationCap size={23} aria-hidden="true" />Mentorias disponíveis</h2>
            <AcademyMentoras />
        </section>
    </AcademyShell>
}