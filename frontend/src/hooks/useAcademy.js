import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { academy } from '../services/academyService'
export function useAcademy(caminho, privado = false, habilitado = true) {
    const [tentativa, setTentativa] = useState(0)
    const [estado, setEstado] = useState({ chave: null, dados: null, erro: '' })
    const chave = caminho + ':' + tentativa
    const navigate = useNavigate()
    const location = useLocation()
    useEffect(() => {
        if (!habilitado) return
        const controller = new AbortController()
        academy(caminho, { privado, signal: controller.signal }).then((dados) => {
            if (!controller.signal.aborted) setEstado({ chave, dados, erro: '' })
        }).catch((falha) => {
            if (falha.name === 'AbortError') return
            if (falha.status === 401) navigate('/login?perfil=usuaria&retorno=' + encodeURIComponent(location.pathname), { replace: true })
            else setEstado({ chave, dados: null, erro: falha.message })
        })
        return () => controller.abort()
    }, [caminho, privado, chave, navigate, location.pathname, habilitado])
    return { dados: estado.chave === chave ? estado.dados : null, erro: estado.chave === chave ? estado.erro : '',
        carregando: habilitado && estado.chave !== chave, repetir: () => setTentativa((t) => t + 1) }
}