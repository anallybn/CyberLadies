import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AreaConta from '../components/AreaConta'
import { listarSolicitacoes } from '../services/educadoraService'

export default function AdminEducadoras() {
    const [filtro, setFiltro] = useState({ status: '', pagina: 0, tamanho: 20 })
    const [resultado, setResultado] = useState(null)
    const [erro, setErro] = useState('')
    const [carregando, setCarregando] = useState(true)
    const [tentativa, setTentativa] = useState(0)
    useEffect(() => {
        const controle = new AbortController()
        listarSolicitacoes(filtro, controle.signal).then(dados => setResultado(dados))
            .catch(falha => { if (falha.name !== 'AbortError') setErro(falha.message) })
            .finally(() => { if (!controle.signal.aborted) setCarregando(false) })
        return () => controle.abort()
    }, [filtro, tentativa])
    function filtrar(novo) { setErro(''); setCarregando(true); setFiltro(novo) }
    const totalPaginas = Math.max(1, Math.ceil((resultado?.total || 0) / filtro.tamanho))
    return <AreaConta titulo="Solicitações de educadora" ativa="admin">
        <p>Consulte os dados profissionais e registre uma decisão para cada solicitação pendente.</p>
        <div className="area-conta__filtros">
            <label>Status <select aria-label="Status" value={filtro.status} onChange={e => filtrar({ ...filtro, status: e.target.value, pagina: 0 })}>
                <option value="">Todos</option><option value="PENDENTE">Pendente</option><option value="APROVADA">Aprovada</option><option value="REJEITADA">Rejeitada</option>
            </select></label>
            <label>Por página <select aria-label="Por página" value={filtro.tamanho} onChange={e => filtrar({ ...filtro, tamanho: Number(e.target.value), pagina: 0 })}>
                <option value="10">10</option><option value="20">20</option><option value="50">50</option>
            </select></label>
        </div>
        {erro && <div role="alert" className="cm-erro">{erro} <button className="cm-botao" onClick={() => { setErro(''); setCarregando(true); setTentativa(t => t + 1) }}>Tentar novamente</button></div>}
        {carregando ? <p role="status">Carregando solicitações...</p> : !erro && resultado && <>
            <p role="status">{resultado.total} solicitação(ões) encontrada(s).</p>
            {resultado.itens.length === 0 ? <div className="cm-cartao">Nenhuma solicitação nesta página.</div> : <div className="cm-cartao area-conta__tabela">
                <table><caption>Solicitações de atuação como educadora</caption><thead><tr><th>Nome</th><th>Especialidade</th><th>Status</th><th>Ação</th></tr></thead>
                    <tbody>{resultado.itens.map(item => <tr key={item.contaId}><td>{item.nome}</td><td>{item.especialidade}</td><td>{item.status}</td>
                        <td><Link to={'/admin/educadoras/' + item.contaId} aria-label={'Analisar solicitação de ' + item.nome}>Consultar</Link></td></tr>)}</tbody>
                </table>
            </div>}
            <nav className="area-conta__acoes" aria-label="Paginação">
                <button className="cm-botao" disabled={filtro.pagina === 0} onClick={() => filtrar({ ...filtro, pagina: filtro.pagina - 1 })}>Anterior</button>
                <span>Página {filtro.pagina + 1} de {totalPaginas}</span>
                <button className="cm-botao" disabled={(filtro.pagina + 1) * filtro.tamanho >= resultado.total} onClick={() => filtrar({ ...filtro, pagina: filtro.pagina + 1 })}>Próxima</button>
            </nav>
        </>}
    </AreaConta>
}
