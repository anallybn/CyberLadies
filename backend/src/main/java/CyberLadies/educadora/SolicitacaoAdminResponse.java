package CyberLadies.educadora;

import java.time.OffsetDateTime;

record SolicitacaoAdminResponse(long contaId, String nome, String especialidade, String instituicao,
                               String status, String motivoRejeicao, Long decididaPor, OffsetDateTime decididaEm) {}
