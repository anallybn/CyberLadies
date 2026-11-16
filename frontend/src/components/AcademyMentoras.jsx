import { Link } from 'react-router-dom'
import { useAcademy } from '../hooks/useAcademy'
import { AcademyEstado } from './AcademyUI'

export default function AcademyMentoras() {
    const estado = useAcademy('/mentoras')
    if (estado.carregando || estado.erro) return <AcademyEstado {...estado} />
    const disponiveis = estado.dados.filter((m) => m.horariosLivres > 0).slice(0, 3)
    if (!disponiveis.length) return <p className="ac-vazio">Nenhuma mentora disponível no momento.</p>
    return <div className="ac-grade">{disponiveis.map((mentora) => <article className="ac-cartao" key={mentora.id}>
        <span className="ac-mentora-avatar" aria-hidden="true">{mentora.nome[0]}</span>
        <h3>{mentora.nome}</h3><p>{mentora.especialidade}</p>
        {mentora.anosExperiencia != null && <small>{mentora.anosExperiencia} anos de experiência</small>}
        <p>{mentora.descricao}</p><span className="ac-tag">{mentora.horariosLivres} {mentora.horariosLivres === 1 ? 'horário livre' : 'horários livres'}</span>
        {mentora.horariosLivres > 0 ? <Link className="ac-botao" to={'/academy/mentorias?mentora=' + mentora.id}>Agendar</Link> :
            <p className="ac-ajuda">Sem novos horários no momento.</p>}
    </article>)}</div>
}