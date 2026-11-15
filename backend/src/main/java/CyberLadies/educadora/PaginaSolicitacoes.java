package CyberLadies.educadora;

import java.util.List;

record PaginaSolicitacoes(List<SolicitacaoAdminResponse> itens, int pagina, int tamanho, long total) {}
