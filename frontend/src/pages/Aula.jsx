import { useConta } from '../hooks/useConta'
import { useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { AcademyShell, AcademyEstado, AcademyVoltar, AcademyProgresso, AcademyPlayer } from '../components/AcademyUI'
import { useAcademy } from '../hooks/useAcademy'
import { academy, duracao } from '../services/academyService'

export default function Aula() {
    const { conta } = useConta()
    const estudante = ['CYBERLADIE', 'EDUCADORA'].includes(conta?.perfil)
    const { id } = useParams()
    const estado = useAcademy('/aulas/' + encodeURIComponent(id), true)
    const meus = useAcademy('/me/progresso', true, estudante)
    const [ocupado, setOcupado] = useState(false)
    const [aviso, setAviso] = useState(null)
    const bloqueio = useRef(false)
    const [reproduzir, setReproduzir] = useState(null)
    const navigate = useNavigate()
    const dados = estado.dados
    const progresso = meus.dados?.find((p) => p.trilhaId === dados?.aula.trilhaId)
    const concluida = progresso?.aulasConcluidas.includes(dados?.aula.id)
    async function concluir() {
        if (bloqueio.current) return
        if (!sessionStorage.getItem('token')) {
            navigate('/login?perfil=usuaria&retorno=' + encodeURIComponent('/academy/aulas/' + id))
            return
        }
        bloqueio.current = true
        setOcupado(true)
        setAviso(null)
        try {
            const resultado = await academy('/aulas/' + id + '/conclusao', { method: 'PUT', privado: true })
            meus.repetir()
            setAviso({ aulaId: id, erro: false, texto: resultado.percentual === 100 ? 'Trilha concluída! Trilhas elegíveis emitem certificado no Perfil.' : 'Aula marcada como concluída.' })
        } catch (falha) {
            if (falha.status === 401) navigate('/login?perfil=usuaria&retorno=' + encodeURIComponent('/academy/aulas/' + id))
            else setAviso({ aulaId: id, erro: true, texto: falha.message })
        } finally { bloqueio.current = false; setOcupado(false) }
    }
    return <AcademyShell><AcademyVoltar />
        {estado.carregando || estado.erro ? <AcademyEstado {...estado} /> : <>
            <Link to={'/academy/trilhas/' + dados.aula.trilhaId}>← Ver trilha e inscrição</Link>
            <h1>{dados.aula.titulo}</h1><p>{duracao(dados.aula.duracaoMinutos)}</p>
            <p className="ac-texto">{dados.aula.descricao}</p>
            <AcademyPlayer titulo={dados.aula.titulo} video={dados.video}
                reproduzir={reproduzir === id} aoCarregar={() => setReproduzir(id)} />
            {dados.conteudoComplementar && <section className="ac-cartao"><h2>Conteúdo complementar</h2><p className="ac-texto">{dados.conteudoComplementar}</p></section>}
            {meus.erro && <AcademyEstado erro={meus.erro} repetir={meus.repetir} />}
            {progresso && <AcademyProgresso percentual={progresso.percentual} />}
            {aviso?.aulaId === id && <p role={aviso.erro ? 'alert' : 'status'} className={'ac-mensagem' + (aviso.erro ? ' ac-erro' : '')}>{aviso.texto}</p>}
            <div className="ac-acoes">
                {progresso ? <button className="ac-botao" disabled={ocupado || concluida || meus.carregando} aria-busy={ocupado} onClick={concluir}>
                    {ocupado ? 'Salvando…' : concluida ? 'Aula concluída' : 'Marcar como concluída'}</button> :
                    <Link className="ac-botao" to={'/academy/trilhas/' + dados.aula.trilhaId}>Inscrever-se para registrar progresso</Link>}
                {dados.proximaAulaId && <Link className="ac-secundario" to={'/academy/aulas/' + dados.proximaAulaId}>Próxima aula →</Link>}
                <Link to="/perfil">Meus certificados</Link>
            </div>
        </>}
    </AcademyShell>
}