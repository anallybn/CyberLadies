
import { Link } from 'react-router-dom'
import TopoLogado from './TopoLogado'
import Circuitos from './Circuitos'

import '../pages/Academy.css'

export function AcademyShell({ children }) {
    return <div className="ac"><TopoLogado ativa="academy" /><Circuitos />
        <main className="ac-conteudo">{children}</main></div>
}
export function AcademyEstado({ carregando, erro, repetir }) {
    return <div className="ac-estado">{carregando ? <p role="status">Carregando CyberAcademy…</p> : <>
        <p role="alert">{erro}</p><button className="ac-botao" onClick={repetir}>Tentar novamente</button></>}</div>
}
export function AcademyProgresso({ percentual = 0 }) {
    return <div className="ac-progresso"><label>Progresso: {percentual}%<progress max={100} value={percentual}>{percentual}%</progress></label></div>
}
export function AcademyVoltar() { return <Link className="ac-voltar" to="/academy">← Voltar ao CyberAcademy</Link> }

