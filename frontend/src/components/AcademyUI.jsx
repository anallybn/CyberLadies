
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
export function AcademyPlayer({ titulo, video, reproduzir, aoCarregar }) {
    return reproduzir ? <iframe className="ac-player" title={'Videoaula: ' + titulo} src={video.url}
        allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture; fullscreen"
        sandbox="allow-scripts allow-same-origin allow-presentation" referrerPolicy="strict-origin-when-cross-origin" allowFullScreen /> :
        <div className="ac-estado"><p>Vídeo hospedado no {video.provedor === 'YOUTUBE' ? 'YouTube' : 'Vimeo'}. Ao carregar, você se conecta ao provedor externo.</p>
            <button className="ac-botao" onClick={aoCarregar}>Carregar vídeo</button></div>
}
export function AcademyProgresso({ percentual = 0 }) {
    return <div className="ac-progresso"><label>Progresso: {percentual}%<progress max={100} value={percentual}>{percentual}%</progress></label></div>
}
export function AcademyVoltar() { return <Link className="ac-voltar" to="/academy">← Voltar ao CyberAcademy</Link> }

