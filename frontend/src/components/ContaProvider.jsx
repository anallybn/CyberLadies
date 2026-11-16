import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import ContaContext from '../context/contaContext'
import { consultarConta } from '../services/authService'
import { useSessao } from '../hooks/useSessao'

export default function ContaProvider({ children }) {
    const { pathname } = useLocation()
    useSessao()
    const token = sessionStorage.getItem('token')
    const [tentativa, setTentativa] = useState(0)
    const [estado, setEstado] = useState({})
    const chave = token + ':' + pathname + ':' + tentativa
    useEffect(() => {
        if (!token) return
        const controle = new AbortController()
        consultarConta(controle.signal).then((conta) => {
            if (controle.signal.aborted || sessionStorage.getItem('token') !== token) return
            setEstado({ chave, conta })
        }).catch((erro) => {
            if (erro.name !== 'AbortError' && !controle.signal.aborted)
                setEstado({ chave, erro: erro.message, expirada: erro.status === 401 })
        })
        return () => controle.abort()
    }, [chave, token])
    const atual = estado.chave === chave
    const valor = {
        conta: token && atual ? estado.conta : null,
        carregando: !!token && !atual,
        erro: token && atual ? estado.erro : null,
        expirada: !token && !!estado.expirada,
        recarregar: () => setTentativa((valor) => valor + 1),
    }
    return <ContaContext.Provider value={valor}>{children}</ContaContext.Provider>
}
