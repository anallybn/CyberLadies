import { useConta } from '../hooks/useConta'
import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { academy } from '../services/academyService'

export default function AcademyIniciar({ trilha, inscrita = false, aoInscrever }) {
    const { conta, carregando, erro: erroConta } = useConta()
    const acessoIndisponivel = !!sessionStorage.getItem('token') && (carregando || !!erroConta || !['CYBERLADIE', 'EDUCADORA'].includes(conta?.perfil))
    const [enviando, setEnviando] = useState(false)
    const [erro, setErro] = useState('')
    const bloqueio = useRef(false)
    const navigate = useNavigate()
    async function iniciar() {
        const destino = '/academy/trilhas/' + trilha.id
        if (!sessionStorage.getItem('token')) {
            navigate('/login?perfil=usuaria&retorno=' + encodeURIComponent(destino))
            return
        }
        if (acessoIndisponivel) return
        if (inscrita) { navigate(destino); return }
        if (bloqueio.current) return
        bloqueio.current = true
        setEnviando(true)
        setErro('')
        try {
            await academy('/trilhas/' + trilha.id + '/inscricoes', { method: 'POST', privado: true })
            aoInscrever?.()
            navigate(destino)
        } catch (falha) {
            if (falha.status === 401) navigate('/login?perfil=usuaria&retorno=' + encodeURIComponent(destino))
            else if (falha.status === 409 && falha.message.includes('já está inscrita')) navigate(destino)
            else setErro(falha.message)
        } finally {
            bloqueio.current = false
            setEnviando(false)
        }
    }
    return <><button type="button" className="ac-botao" disabled={enviando || !trilha.totalAulas || acessoIndisponivel} aria-busy={enviando} onClick={iniciar}>
        {carregando ? 'Confirmando conta…' : erroConta ? 'Conta não confirmada' : acessoIndisponivel ? 'Acesso de estudante necessário' : enviando ? 'Inscrevendo…' : !trilha.totalAulas ? 'Conteúdo em preparação' : inscrita ? 'Continuar trilha' : 'Iniciar trilha'}
    </button>{erro && <p role="alert" className="ac-erro">{erro}</p>}</>
}