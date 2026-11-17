import { Link } from 'react-router-dom'
import { useConta } from '../hooks/useConta'
import RotaProtegida from './RotaProtegida'
import AreaConta from './AreaConta'

function ConferirAprovacao({ children }) {
    const { conta, carregando, erro, recarregar } = useConta()
    if (carregando || !conta && !erro) return <AreaConta titulo="Verificando aprovação"><p role="status">Carregando sua conta...</p></AreaConta>
    if (erro) return <AreaConta titulo="Não foi possível verificar sua aprovação">
        <p role="alert">{erro}</p><button className="cm-botao" onClick={recarregar}>Tentar novamente</button>
    </AreaConta>
    if (conta?.statusEducadora !== 'APROVADA') return <AreaConta titulo="Aprovação necessária">
        <p role="alert">A área da educadora está disponível somente após aprovação da solicitação.</p>
        <Link to="/educadora">Consultar minha solicitação</Link>
    </AreaConta>
    return children
}

export default function RotaEducadoraAprovada({ children }) {
    return <RotaProtegida papeis={['EDUCADORA']}><ConferirAprovacao>{children}</ConferirAprovacao></RotaProtegida>
}
