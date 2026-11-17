import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import AreaConta from '../components/AreaConta'
import EstadoSolicitacao from '../components/EstadoSolicitacao'
import { consultarAnalise, decidirSolicitacao } from '../services/educadoraService'

export default function AdminEducadora() {
    const { id } = useParams()
    const [solicitacao, setSolicitacao] = useState(null)
    const [carregando, setCarregando] = useState(true)
    const [erro, setErro] = useState('')
    const [aviso, setAviso] = useState('')
    const [tentativa, setTentativa] = useState(0)
    const [enviando, setEnviando] = useState(false)
    const [decisao, setDecisao] = useState('APROVADA')
    const [motivo, setMotivo] = useState('')
    useEffect(() => {
        const controle = new AbortController()
        consultarAnalise(id, controle.signal).then(dados => setSolicitacao(dados))
            .catch(falha => { if (falha.name !== 'AbortError') { setErro(falha.message); setSolicitacao(null) } })
            .finally(() => { if (!controle.signal.aborted) setCarregando(false) })
        return () => controle.abort()
    }, [id, tentativa])
    function atualizar() { setErro(''); setCarregando(true); setTentativa(t => t + 1) }
    async function enviar(evento) {
        evento.preventDefault()
        if (enviando) return
        if (decisao === 'REJEITADA' && !motivo.trim()) { setErro('Informe o motivo da rejeição.'); return }
        setEnviando(true); setErro(''); setAviso('')
        try {
            setSolicitacao(await decidirSolicitacao(id, decisao, motivo.trim()))
            setAviso('Decisão registrada.')
        } catch (falha) {
            setErro(falha.message)
            if (falha.status === 409) {
                setAviso('A solicitação já foi analisada. O resultado atual foi recarregado.')
                try { setSolicitacao(await consultarAnalise(id)) }
                catch (consultaErro) { setSolicitacao(null); setErro(consultaErro.message) }
            }
        } finally { setEnviando(false) }
    }
    return <AreaConta titulo="Análise da solicitação" ativa="admin">
        <Link to="/admin/educadoras">Voltar às solicitações</Link>
        {carregando && <p role="status">Carregando solicitação...</p>}
        {aviso && <p role="status">{aviso}</p>}
        {erro && <div className="cm-erro" role="alert">{erro} <button className="cm-botao" onClick={atualizar}>Atualizar solicitação</button></div>}
        {!carregando && solicitacao && <section className="cm-cartao area-conta__secao">
            <h2>{solicitacao.nome}</h2><EstadoSolicitacao solicitacao={solicitacao} />
            {solicitacao.decididaPor && <p>Responsável pela decisão: conta {solicitacao.decididaPor}</p>}
            {solicitacao.status === 'PENDENTE' ? <form className="area-conta__form" onSubmit={enviar}>
                <fieldset disabled={enviando}>
                    <label htmlFor="decisao">Decisão</label><select id="decisao" value={decisao} onChange={e => setDecisao(e.target.value)}>
                        <option value="APROVADA">Aprovar</option><option value="REJEITADA">Rejeitar</option>
                    </select>
                    {decisao === 'REJEITADA' && <><label htmlFor="motivo">Motivo da rejeição</label>
                        <textarea id="motivo" required maxLength={1000} value={motivo} onChange={e => setMotivo(e.target.value)} /></>}
                    <p>A decisão é definitiva nesta etapa. Confira os dados antes de confirmar.</p>
                    <button type="submit" className="cm-botao" disabled={enviando}>{enviando ? 'Registrando...' : 'Confirmar decisão'}</button>
                </fieldset>
            </form> : <p>Solicitação analisada. Não há reanálise ou revogação disponível.</p>}
        </section>}
    </AreaConta>
}
