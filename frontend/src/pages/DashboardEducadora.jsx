import { API_BASE_URL as API } from '../services/apiUrl.js'
import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { UserRound, CalendarDays, BookOpen } from 'lucide-react'
import TopoEducadora from '../components/TopoEducadora'
import './DashboardEducadora.css'

const numero = (valor) => new Intl.NumberFormat('pt-BR').format(valor)
const data = (valor) => new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(valor))

export default function DashboardEducadora() {
    const [estado, setEstado] = useState({ carregando: true })
    const [tentativa, setTentativa] = useState(0)
    useEffect(() => {
        const controle = new AbortController()
        async function carregar() {
            try {
                const resposta = await fetch(API + '/educadora/dashboard', {
                    headers: { Authorization: 'Bearer ' + (sessionStorage.getItem('token') || '') },
                    signal: controle.signal,
                })
                if (resposta.status === 401 || resposta.status === 403) {
                    setEstado({ acesso: resposta.status }); return
                }
                if (!resposta.ok) throw new Error('Não foi possível carregar seu dashboard.')
                const dados = await resposta.json()
                setEstado({ dados })
            } catch (erro) {
                if (erro.name !== 'AbortError') setEstado({ erro: erro.message })
            }
        }
        carregar()
        return () => controle.abort()
    }, [tentativa])
    if (estado.acesso === 401) return <Navigate to="/login?perfil=educadora" replace />
    const dados = estado.dados
    const maximo = Math.max(1, ...(dados?.trilhas || []).map((trilha) => trilha.inscricoes))
    return <div className="ed-dashboard ed-painel">
        <TopoEducadora />
        <main className="ed-conteudo">
            {estado.carregando && <p role="status">Carregando seu dashboard…</p>}
            {estado.acesso === 403 && <div className="ed-estado" role="alert"><h1>Acesso exclusivo da Educadora</h1><p>Esta conta não possui acesso a este dashboard.</p><Link to="/academy">Acessar CyberAcademy</Link></div>}
            {estado.erro && <div className="ed-estado" role="alert"><p>{estado.erro}</p><button type="button" onClick={() => { setEstado({ carregando: true }); setTentativa((n) => n + 1) }}>Tentar novamente</button></div>}
            {dados && <>
                <section className="ed-saudacao">
                    <h1><span className="ed-boasvindas-icone" aria-hidden="true"><BookOpen size={21} strokeWidth={1.8}/></span><span className="ed-boasvindas-texto"><span className="ed-boasvindas-prefixo">Bem-vinda, </span><span className="ed-boasvindas-nome">{dados.nome}!</span></span></h1>
                    <p>Aqui está um resumo da sua atividade no CyberAcademy.</p>
                    <p className="ed-status">Conta {dados.statusEducadora.toLocaleLowerCase('pt-BR')}. Publicação de conteúdos ainda indisponível.</p>
                </section>
                <dl className="ed-indicadores">
                    {[
                        [numero(dados.indicadores.materiaisPublicados), 'Materiais publicados'],
                        [numero(dados.indicadores.alunasAlcancadas), 'Alunas alcançadas'],
                        [numero(dados.indicadores.conclusoes), 'Conclusões'],
                        ['Em breve', 'Avaliação média'],
                        ['Em breve', 'Visualizações deste mês'],
                    ].map(([valor, rotulo]) => <div key={rotulo} className={valor === 'Em breve' ? 'ed-indicador-futuro' : undefined}><dt>{rotulo}</dt><dd>{valor}</dd></div>)}
                </dl>
                <section className="ed-secao">
                    <h2>Alunas por trilha</h2>
                    <div className="ed-grafico" role="group" aria-label="Quantidade de inscrições por trilha. Valores detalhados na lista.">
                        {dados.trilhas.length ? <ul>{dados.trilhas.map((trilha, i) => <li key={trilha.id}>
                            <span className="ed-trilha-nome" title={trilha.titulo}>{trilha.titulo}</span>
                            <span className={'ed-barra ed-barra--' + i % 4}><span style={{ width: (trilha.inscricoes / maximo * 100) + '%' }} /></span>
                            <span className="ed-trilha-total">{numero(trilha.inscricoes)}</span>
                        </li>)}</ul> : <p className="ed-vazio">Suas trilhas aparecerão aqui quando houver conteúdos vinculados à sua conta.</p>}
                    </div>
                </section>
                <section className="ed-secao">
                    <h2>Próximas mentorias agendadas</h2>
                    <div className="ed-mentorias">
                        {dados.mentorias.length ? <ul>{dados.mentorias.map((mentoria) => <li key={mentoria.id}>
                            <span className="ed-aluna-avatar" aria-hidden="true"><UserRound size={20} /></span>
                            <div><strong>{mentoria.aluna}</strong><span>{mentoria.tema}</span></div>
                            <time dateTime={mentoria.inicio}>{data(mentoria.inicio)}<small>até {new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(mentoria.fim))}</small></time>
                        </li>)}</ul> : <p className="ed-vazio"><CalendarDays size={22} aria-hidden="true" />Nenhuma mentoria confirmada para os próximos dias.</p>}
                        <span className="ed-fuso">Horários no fuso do seu dispositivo.</span>
                    </div>
                </section>
                <section className="ed-secao">
                    <h2>Materiais recentes</h2>
                    <div className="ed-tabela-container" tabIndex={0} role="region" aria-label="Materiais recentes, deslize para ver todas as colunas">
                        <table className="ed-tabela">
                            <thead><tr><th scope="col">Materiais</th><th scope="col">Trilha</th><th scope="col" title="Total de aulas da trilha">Aulas</th><th scope="col">Avaliação</th><th scope="col">Status</th></tr></thead>
                            <tbody>{dados.materiais.map((material) => <tr key={material.id}><td>{material.titulo}</td><td>{material.trilha}</td><td aria-label={material.aulas + ' aulas na trilha'}>{numero(material.aulas)}</td><td>Em breve</td><td>{material.status}</td></tr>)}
                                {!dados.materiais.length && <tr><td colSpan={5} className="ed-vazio">Nenhum material vinculado à sua conta.</td></tr>}
                            </tbody>
                        </table>
                    </div>
                </section>
            </>}
        </main>
    </div>
}
