import './TituloEducadora.css'

export default function TituloEducadora({ icone: Icone, inicio, destaque, subtitulo }) {
    return <>
        <h1 className="titulo-educadora">
            <span className="titulo-educadora__icone" aria-hidden="true">
                <Icone size={21} strokeWidth={1.8} />
            </span>
            <span><span className="titulo-educadora__inicio">{inicio}</span>{' '}<span className="titulo-educadora__destaque">{destaque}</span></span>
        </h1>
        <p className="titulo-educadora__subtitulo">{subtitulo}</p>
    </>
}
