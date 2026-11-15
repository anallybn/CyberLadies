import { useEffect, useState } from 'react'
import AreaConta from '../components/AreaConta'
import EstadoSolicitacao from '../components/EstadoSolicitacao'
import { consultarSolicitacao, solicitarEducadora } from '../services/educadoraService'
import { atualizarSessao } from '../services/sessaoService'

export default function Educadora() {
    const [solicitacao, setSolicitacao] = useState(null)
    const [carregando, setCarregando] = useState(true)
    const [erro, setErro] = useState('')
    const [falhaConsulta, setFalhaConsulta] = useState(false)
    const [tentativa, setTentativa] = useState(0)
    const [enviando, setEnviando] = useState(false)
    const [especialidade, setEspecialidade] = useState('')
    const [instituicao, setInstituicao] = useState('')
    const [aviso, setAviso] = useState('')
    useEffect(() => {
        let ativa = true
        consultarSolicitacao().then(dados => { if (ativa) setSolicitacao(dados) })
            .catch(falha => { if (ativa) { setErro(falha.message); setFalhaConsulta(true) } })
            .finally(() => { if (ativa) setCarregando(false) })
        return () => { ativa = false }
    }, [tentativa])
    function atualizar() { setErro(''); setAviso(''); setFalhaConsulta(false); setCarregando(true); setTentativa(valor => valor + 1) }
    async function enviar(evento) {
        evento.preventDefault()
        if (enviando) return
        if (!especialidade.trim() || !instituicao.trim()) { setErro('Informe especialidade e instituição.'); return }
        setEnviando(true); setErro(''); setAviso('')
        try {
            setSolicitacao(await solicitarEducadora(especialidade.trim(), instituicao.trim()))
            setAviso('Solicitação enviada para análise.')
            await atualizarSessao()
        } catch (falha) {
            if (falha.status === 409) {
                try { setSolicitacao(await consultarSolicitacao()); setAviso('Sua solicitação já existe. O status atual foi carregado.') }
                catch (consultaErro) { setErro(consultaErro.message); setFalhaConsulta(true) }
            } else setErro(falha.message)
        } finally { setEnviando(false) }
    }
    return <AreaConta titulo="Atuar como educadora" ativa="educadora">
        <p>Use sua conta pessoal para solicitar atuação como educadora. Sua conta e seu acesso à comunidade continuam os mesmos.</p>
        {aviso && <p role="status">{aviso}</p>}
        {erro && <div className="cm-erro" role="alert">{erro} <button className="cm-botao" onClick={atualizar}>Tentar novamente</button></div>}
        {carregando ? <p role="status">Carregando solicitação...</p> : solicitacao ? <section className="cm-cartao area-conta__secao">
            <h2>Minha solicitação</h2><EstadoSolicitacao solicitacao={solicitacao} />
            {solicitacao.status === 'PENDENTE' && <p>Sua solicitação está em análise. Aguarde uma decisão.</p>}
            {solicitacao.status === 'APROVADA' && <p>Sua atuação como educadora foi aprovada.</p>}
            {solicitacao.status === 'REJEITADA' && <p>A solicitação foi rejeitada. Não há reenvio ou reanálise disponível nesta etapa.</p>}
            <button className="cm-botao cm-botao--contorno" onClick={atualizar}>Atualizar status</button>
        </section> : !falhaConsulta && <form className="cm-cartao area-conta__form" onSubmit={enviar}>
            <h2>Solicitar atuação</h2>
            <p>Informe seus dados profissionais. Não é necessário criar outra conta.</p>
            <fieldset disabled={enviando}>
                <label htmlFor="especialidade">Especialidade</label>
                <input id="especialidade" required maxLength={120} value={especialidade} onChange={e => setEspecialidade(e.target.value)} />
                <label htmlFor="instituicao">Instituição</label>
                <input id="instituicao" required maxLength={200} value={instituicao} onChange={e => setInstituicao(e.target.value)} />
                <button className="cm-botao" type="submit" disabled={enviando}>{enviando ? 'Enviando...' : 'Enviar solicitação'}</button>
            </fieldset>
        </form>}
    </AreaConta>
}
