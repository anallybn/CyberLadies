import { useConta } from '../hooks/useConta'
import { Link, useParams } from 'react-router-dom'
import { AcademyShell, AcademyEstado, AcademyVoltar, AcademyProgresso } from '../components/AcademyUI'
import AcademyIniciar from '../components/AcademyIniciar'
import { useAcademy } from '../hooks/useAcademy'
import { CATEGORIAS, NIVEIS, duracao } from '../services/academyService'

export default function Trilha() {
    const { conta } = useConta()
    const estudante = ['CYBERLADIE', 'EDUCADORA'].includes(conta?.perfil)
    const { id } = useParams()
    const trilha = useAcademy('/trilhas/' + encodeURIComponent(id))
    const aulas = useAcademy('/trilhas/' + encodeURIComponent(id) + '/aulas')
    const meus = useAcademy('/me/progresso', true, estudante)
    const progresso = meus.dados?.find((p) => String(p.trilhaId) === id)
    const dados = trilha.dados
    const falha = trilha.erro || aulas.erro || meus.erro
    return <AcademyShell><AcademyVoltar />
        {trilha.carregando || aulas.carregando || falha ? <AcademyEstado carregando={trilha.carregando || aulas.carregando}
            erro={falha} repetir={() => { trilha.repetir(); aulas.repetir(); meus.repetir() }} /> : <>
            <section className="ac-cartao"><span className="ac-tag">{CATEGORIAS[dados.categoria]} · {NIVEIS[dados.nivel]} · Versão {dados.versao}</span>
                <h1>{dados.titulo}</h1><p className="ac-texto">{dados.descricao}</p>
                <p>{dados.totalAulas} aulas · {duracao(dados.cargaMinutos)}</p>
                {progresso ? <><AcademyProgresso percentual={progresso.percentual} />
                    {progresso.proximaAulaId ? <Link className="ac-botao" to={'/academy/aulas/' + progresso.proximaAulaId}>Continuar estudos</Link> :
                        <p role="status">Trilha concluída! {dados.elegivelCertificado ? 'Seu certificado está disponível no Perfil.' : ''}</p>}</> :
                    <AcademyIniciar key={meus.dados ? 'carregado' : 'inicial'} trilha={dados} aoInscrever={meus.repetir} />}
                <p className="ac-ajuda">{dados.elegivelCertificado ? 'Emite certificado ao concluir todas as aulas.' : 'Esta trilha não emite certificado.'}</p>
            </section>
            <section aria-labelledby="ac-conteudo-trilha"><h2 id="ac-conteudo-trilha">Sua jornada de aprendizagem</h2>
                {!aulas.dados.length ? <p className="ac-vazio">Nenhuma aula disponível nesta trilha.</p> :
                    <ol className="ac-lista">{aulas.dados.map((aula) => <li key={aula.id}>
                        <div><Link to={'/academy/aulas/' + aula.id}>{aula.ordem}. {aula.titulo}</Link><p className="ac-ajuda">{duracao(aula.duracaoMinutos)}</p></div>
                        <span className="ac-tag">{progresso?.aulasConcluidas.includes(aula.id) ? 'Concluída' : 'Não iniciada'}</span>
                    </li>)}</ol>}
            </section>
        </>}
    </AcademyShell>
}