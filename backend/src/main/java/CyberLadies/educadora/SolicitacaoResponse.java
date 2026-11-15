package CyberLadies.educadora;

import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;

record SolicitacaoResponse(String especialidade, String instituicao, String status,
                          @JsonInclude(JsonInclude.Include.NON_NULL) String motivoRejeicao,
                          @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime decididaEm) {}
