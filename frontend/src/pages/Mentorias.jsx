import { useConta } from '../hooks/useConta'
import { useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { AcademyShell, AcademyEstado, AcademyVoltar } from '../components/AcademyUI'
import { useAcademy } from '../hooks/useAcademy'
import { academy, horario } from '../services/academyService'

export default function Mentorias() {
    const { conta } = useConta()
    const estudante = ['CYBERLADIE', 'EDUCADORA'].includes(conta?.perfil)
    const [params] = useSearchParams()
    const [mentora, setMentora] = useState(params.get('mentora') || '')
    const [disponibilidade, setDisponibilidade] = useState('')
    const [ocupado, setOcupado] = useState(false)
    const [confirmar, setConfirmar] = useState(null)
    const [aviso, setAviso] = useState(null)
    const bloqueio = useRef(false)
    const navigate = useNavigate()
    const mentoras = useAcademy('/mentoras')
    const horarios = useAcademy('/mentoras/' + encodeURIComponent(mentora) + '/horarios', false, !!mentora)
    const meus = useAcademy('/me/agendamentos', true, estudante)
    async function executar(caminho, method, dados) {
        if (bloqueio.current) return
        if (!estudante) { navigate('/login?perfil=usuaria&retorno=%2Facademy%2Fmentorias'); return }
        bloqueio.current = true
        setOcupado(true)
        setAviso(null)
        try {
            await academy(caminho, { method, dados, privado: true })
            setAviso({ erro: false, texto: method === 'POST' ? 'Agendamento confirmado.' : 'Agendamento cancelado.' })
            setDisponibilidade('')
            setConfirmar(null)
            horarios.repetir()
            meus.repetir()
            mentoras.repetir()
        } catch (falha) {
            if (falha.status === 401) navigate('/login?perfil=usuaria&retorno=%2Facademy%2Fmentorias')
            else { setAviso({ erro: true, texto: falha.message }); horarios.repetir() }
        } finally { bloqueio.current = false; setOcupado(false) }
    }
    return <AcademyShell><AcademyVoltar /><h1>Mentorias para o seu próximo passo</h1>
        <p>Escolha uma mentora e reserve um horário disponível. A confirmação é imediata e o cancelamento é permitido até antes do início.</p>
        {aviso && <p role={aviso.erro ? 'alert' : 'status'} className={'ac-mensagem' + (aviso.erro ? ' ac-erro' : '')}>{aviso.texto}</p>}
        {mentoras.carregando || mentoras.erro ? <AcademyEstado {...mentoras} /> : !mentoras.dados.length ?
            <p className="ac-vazio">Nenhuma mentora cadastrada no momento.</p> : <section className="ac-cartao">
                <h2>Encontre seu horário</h2>
                <form onSubmit={(e) => { e.preventDefault(); if (disponibilidade) executar('/agendamentos', 'POST', { disponibilidadeId: Number(disponibilidade) }) }}>
                    <fieldset className="ac-agenda-form" disabled={ocupado}>
                        <legend className="ac-sr">Agendar mentoria</legend>
                        <label>Mentora<select required value={mentora} onChange={(e) => { setMentora(e.target.value); setDisponibilidade('') }}>
                            <option value="">Selecione uma mentora</option>{mentoras.dados.map((m) => <option key={m.id} value={m.id}>{m.nome} · {m.especialidade}</option>)}</select></label>
                        {mentora && (horarios.carregando || horarios.erro ? <AcademyEstado {...horarios} /> : !horarios.dados?.length ?
                            <p className="ac-vazio">Nenhum horário disponível para esta mentora.</p> : <label>Horário (Brasília)<select required value={disponibilidade} onChange={(e) => setDisponibilidade(e.target.value)}>
                                <option value="">Selecione um horário</option>{horarios.dados.map((h) => <option key={h.id} value={h.id}>{horario(h.inicio)} até {horario(h.fim)}</option>)}</select></label>)}
                        <button type="submit" className="ac-botao" disabled={!disponibilidade || ocupado}>{ocupado ? 'Processando…' : 'Confirmar agendamento'}</button>
                    </fieldset>
                </form>
            </section>}
        <section aria-labelledby="ac-meus-agendamentos"><h2 id="ac-meus-agendamentos">Meus agendamentos</h2>
            {!estudante ? <p className="ac-vazio"><Link to="/login?perfil=usuaria&retorno=%2Facademy%2Fmentorias">Entre para consultar seus agendamentos.</Link></p> :
                meus.carregando || meus.erro ? <AcademyEstado {...meus} /> : !meus.dados?.length ?
                    <p className="ac-vazio">Você ainda não possui agendamentos.</p> : <ul className="ac-lista">{meus.dados.map((a) => <li key={a.id}>
                        <div><h3>{a.mentora}</h3><p>{horario(a.inicio)} até {horario(a.fim)} (Brasília)</p><span className="ac-tag">{a.status === 'CANCELADO' ? 'Cancelado' : 'Confirmado'}</span></div>
                        {a.status === 'CONFIRMADO' && new Date(a.inicio) > new Date() && (confirmar === a.id ?
                            <div className="ac-acoes"><span>Cancelar esta reserva?</span><button className="ac-botao" disabled={ocupado} onClick={() => executar('/agendamentos/' + a.id, 'DELETE')}>Confirmar cancelamento</button>
                                <button className="ac-secundario" disabled={ocupado} onClick={() => setConfirmar(null)}>Manter reserva</button></div> :
                            <button className="ac-secundario" disabled={ocupado} onClick={() => setConfirmar(a.id)}>Cancelar agendamento</button>)}
                    </li>)}</ul>}
        </section>
    </AcademyShell>
}