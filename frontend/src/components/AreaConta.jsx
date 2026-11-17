import TopoLogado from './TopoLogado'
import Circuitos from './Circuitos'
import '../pages/Comunidade.css'
import '../pages/AreaConta.css'
export default function AreaConta({ titulo, ativa, children }) {
    return <div className="cm area-conta"><TopoLogado ativa={ativa} /><Circuitos />
        <main className="area-conta__conteudo"><header><h1>{titulo}</h1></header>{children}</main>
    </div>
}
