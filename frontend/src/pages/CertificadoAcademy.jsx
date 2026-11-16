import { useParams } from 'react-router-dom'
import { AcademyShell, AcademyEstado, AcademyVoltar } from '../components/AcademyUI'
import { useAcademy } from '../hooks/useAcademy'
import { duracao, horario } from '../services/academyService'

export default function CertificadoAcademy() {
    const { id } = useParams()
    const estado = useAcademy('/me/certificados/' + encodeURIComponent(id), true)
    const dados = estado.dados
    return <AcademyShell><AcademyVoltar />
        {estado.carregando || estado.erro ? <AcademyEstado {...estado} /> : <article className="ac-cartao">
            <span className="ac-etiqueta">CYBERACADEMY · REGISTRO DE CONCLUSÃO</span>
            <h1>Certificado da plataforma</h1><h2>{dados.nomeUsuaria}</h2>
            <p>Concluiu todas as {dados.totalAulas} aulas de <strong>{dados.tituloTrilha}</strong>, versão {dados.versao}, com carga de {duracao(dados.cargaMinutos)}.</p>
            <p>Emitido em {horario(dados.emitidoEm)} (Brasília).</p>
            <p className="ac-certificado">Identificador único: {dados.id}</p>
        </article>}
    </AcademyShell>
}