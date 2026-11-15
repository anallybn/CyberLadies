const nomes = { PENDENTE: 'Pendente', APROVADA: 'Aprovada', REJEITADA: 'Rejeitada' }
export default function EstadoSolicitacao({ solicitacao }) {
    return <div className="solicitacao-dados">
        <span className={'solicitacao-status solicitacao-status--' + solicitacao.status.toLowerCase()}>{nomes[solicitacao.status] || solicitacao.status}</span>
        <dl><div><dt>Especialidade</dt><dd>{solicitacao.especialidade}</dd></div>
            <div><dt>Instituição</dt><dd>{solicitacao.instituicao}</dd></div>
            {solicitacao.decididaEm && <div><dt>Data da decisão</dt><dd>{new Date(solicitacao.decididaEm).toLocaleString('pt-BR')}</dd></div>}
            {solicitacao.motivoRejeicao && <div><dt>Motivo da rejeição</dt><dd>{solicitacao.motivoRejeicao}</dd></div>}
        </dl>
    </div>
}
