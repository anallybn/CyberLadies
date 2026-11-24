import { useEffect, useRef, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { BookOpen, Eye, Info, Search, SearchX, Users, X } from 'lucide-react'
import TopoLogado from '../components/TopoLogado'
import TituloEducadora from '../components/TituloEducadora'
import { useConta } from '../hooks/useConta'
import { consultarAlunas } from '../services/alunasService'
import './MinhasAlunas.css'

const numero=(valor)=>new Intl.NumberFormat('pt-BR').format(valor)
const data=(valor)=>new Intl.DateTimeFormat('pt-BR',{dateStyle:'short',timeZone:'America/Sao_Paulo'}).format(new Date(valor))
function Barra({ inscricao }) {
    return <span className="ma-progresso"><progress max={100} value={inscricao.percentual} aria-label={inscricao.aulasConcluidas+' de '+inscricao.totalAulas+' aulas concluídas'} /><span>{inscricao.percentual}%</span></span>
}
function Detalhe({ inscricao, fechar }) {
    const dialogo=useRef(null)
    const [estado,setEstado]=useState({})
    const [tentativa,setTentativa]=useState(0)
    useEffect(()=>{
        const elemento=dialogo.current;elemento.showModal()
        return ()=>elemento.close()
    },[])
    useEffect(()=>{
        const controle=new AbortController()
        consultarAlunas('/'+inscricao.alunaId+'/trilhas/'+inscricao.trilhaId+'/progresso',controle.signal)
            .then(dados=>{if(!controle.signal.aborted)setEstado({dados,tentativa})})
            .catch(erro=>{if(erro.name!=='AbortError')setEstado({erro:erro.message,tentativa})})
        return ()=>controle.abort()
    },[inscricao.alunaId,inscricao.trilhaId,tentativa])
    const atual=estado.tentativa===tentativa
    return <dialog ref={dialogo} className="ma-dialogo" aria-labelledby="ma-detalhe-titulo" onCancel={fechar}>
        <button type="button" className="ma-fechar" onClick={fechar} aria-label="Fechar progresso" autoFocus><X size={20} /></button>
        <h2 id="ma-detalhe-titulo">Progresso da inscrição</h2>
        {!atual?<p role="status">Carregando progresso…</p>:estado.erro?<div role="alert"><p>{estado.erro}</p><button type="button" onClick={()=>setTentativa(v=>v+1)}>Tentar novamente</button></div>:<>
            <h3>{estado.dados.inscricao.nome}</h3><p>{estado.dados.inscricao.trilha}</p>
            <Barra inscricao={estado.dados.inscricao} />
            <p>{estado.dados.inscricao.aulasConcluidas} de {estado.dados.inscricao.totalAulas} aulas concluídas</p>
            <p>Inscrição em {data(estado.dados.inscricao.inscritaEm)}</p>
            <ol>{estado.dados.aulas.map(aula=><li key={aula.id}><span>{aula.titulo}</span><small>{aula.concluida?'Concluída em '+data(aula.concluidaEm):'Ainda não concluída'}</small></li>)}</ol>
        </>}
    </dialog>
}
export default function MinhasAlunas(){
    const {conta,carregando,erro:erroConta,expirada,recarregar}=useConta()
    const aprovada=conta?.perfil==='EDUCADORA'&&conta.statusEducadora==='APROVADA'
    const [busca,setBusca]=useState(''),[status,setStatus]=useState('TODAS'),[trilha,setTrilha]=useState('')
    const [estado,setEstado]=useState({}),[tentativa,setTentativa]=useState(0),[selecionada,setSelecionada]=useState(null)
    const parametros=new URLSearchParams({busca,status,...(trilha?{trilhaId:trilha}:{})}).toString()
    const chave=conta?.id+':'+parametros+':'+tentativa
    useEffect(()=>{
        if(!aprovada)return
        const controle=new AbortController()
        const timer=setTimeout(()=>{
            consultarAlunas('?'+parametros,controle.signal).then(dados=>{if(!controle.signal.aborted)setEstado({chave,dados})})
                .catch(erro=>{if(erro.name!=='AbortError')setEstado({chave,erro:erro.message,acesso:erro.status})})
        },250)
        return ()=>{clearTimeout(timer);controle.abort()}
    },[aprovada,chave,parametros])
    const atual=aprovada&&estado.chave===chave
    const dados=atual?estado.dados:null,erro=erroConta||(atual?estado.erro:null),acesso=atual?estado.acesso:null
    if(expirada||acesso===401||(!sessionStorage.getItem('token')&&!carregando))return <Navigate to="/login?perfil=educadora" replace />
    const restrita=(conta&&!aprovada)||acesso===403
    const carregandoLista=!restrita&&!erro&&(carregando||!dados)
    function limpar(){setBusca('');setStatus('TODAS');setTrilha('')}
    return <div className="ed-dashboard ma-pagina"><TopoLogado ativa="alunas" />
        <main className="ed-conteudo ma-conteudo">
            <header className="ma-apresentacao">
                <TituloEducadora icone={Users} inicio="Minhas" destaque="Alunas"
                    subtitulo="Acompanhe o progresso das alunas nos seus materiais." />
            </header>
            {restrita?<section className="ma-estado" role="status"><Info size={30} aria-hidden="true" /><h2>{conta?.perfil==='EDUCADORA'?'Acompanhamento disponível após aprovação':'Acesso exclusivo da Educadora'}</h2><p>{conta?.perfil==='EDUCADORA'?'Sua conta precisa estar aprovada para consultar dados de inscrições e progresso das alunas. Seus acessos atuais continuam disponíveis.':'Somente Educadoras aprovadas podem consultar esta área.'}</p></section>:<>
                <div className="ma-pesquisa"><Search size={18} aria-hidden="true" /><label className="ma-sr" htmlFor="ma-busca">Buscar aluna por nome</label><input id="ma-busca" type="search" maxLength={160} value={busca} onChange={e=>setBusca(e.target.value)} placeholder="Buscar aluna..." /></div>
                <dl className="ma-indicadores">{[
                    ['totalAlunas','Total de alunas distintas'],['inscricoesConcluidas','Inscrições concluídas'],['inscricoesEmAndamento','Inscrições em andamento'],['inscricoes','Inscrições nas trilhas']
                ].map(([campo,rotulo])=><div key={campo}><dt>{rotulo}</dt><dd>{dados?numero(dados.indicadores[campo]):'—'}</dd></div>)}</dl>
                <p className="ma-legenda">Indicadores de todas as suas trilhas. Cada aluna é contada uma vez no primeiro card; os demais contam inscrições. Em andamento inclui progresso zero.</p>
                <div className="ma-filtros"><div role="group" aria-label="Filtrar por progresso">{[['TODAS','Todas'],['ANDAMENTO','Em progresso'],['CONCLUIDAS','Concluíram']].map(([valor,rotulo])=><button type="button" key={valor} aria-pressed={status===valor} onClick={()=>setStatus(valor)}>{rotulo}</button>)}</div>
                    {!!dados&&dados.trilhas.length>1&&<label htmlFor="ma-trilha">Trilha<select id="ma-trilha" value={trilha} onChange={e=>setTrilha(e.target.value)}><option value="">Todas as minhas trilhas</option>{dados.trilhas.map(t=><option key={t.id} value={t.id}>{t.titulo}</option>)}</select></label>}
                </div>
                <section className="ma-acompanhamento"><h2>Acompanhamento das Alunas</h2>
                    {erro?<div className="ma-estado" role="alert"><Info size={30} /><h3>Não foi possível carregar as alunas</h3><p>{erro}</p><button type="button" onClick={()=>erroConta?recarregar():setTentativa(v=>v+1)}>Tentar novamente</button></div>:carregandoLista?<div className="ma-estado" role="status" aria-busy="true"><p>Carregando inscrições e progresso…</p></div>:!dados.trilhas.length?<div className="ma-estado"><BookOpen size={30} /><h3>Nenhuma trilha vinculada</h3><p>Quando houver trilhas vinculadas à sua conta e inscrições, o acompanhamento aparecerá aqui.</p></div>:!dados.indicadores.inscricoes?<div className="ma-estado"><Users size={30} /><h3>Ainda não há alunas inscritas</h3><p>As inscrições nas suas trilhas serão exibidas aqui.</p></div>:!dados.inscricoes.length?<div className="ma-estado"><SearchX size={30} /><h3>Nenhuma aluna encontrada</h3><p>Experimente outro nome ou ajuste os filtros.</p><button type="button" onClick={limpar}>Limpar filtros</button></div>:
                    <div className="ma-tabela-container"><table className="ma-tabela"><caption className="ma-sr">Uma linha por inscrição. A mesma aluna pode aparecer em mais de uma trilha.</caption><thead><tr><th scope="col">Aluna</th><th scope="col">Trilha</th><th scope="col">Progresso</th><th scope="col">Data da inscrição</th><th scope="col">Ações</th></tr></thead><tbody>{dados.inscricoes.map(i=><tr key={i.alunaId+':'+i.trilhaId}>
                        <td data-label="Aluna">{i.nome}</td><td data-label="Trilha">{i.trilha}</td><td data-label="Progresso"><Barra inscricao={i} /></td><td data-label="Data da inscrição"><time dateTime={i.inscritaEm}>{data(i.inscritaEm)}</time></td><td data-label="Ações"><button type="button" className="ma-ver" onClick={()=>setSelecionada(i)}><Eye size={14} aria-hidden="true" />Ver progresso</button></td>
                    </tr>)}</tbody></table></div>}
                </section>
            </>}
        </main>
        {selecionada&&aprovada&&<Detalhe key={conta.id+':'+selecionada.alunaId+':'+selecionada.trilhaId} inscricao={selecionada} fechar={()=>setSelecionada(null)} />}
    </div>
}
