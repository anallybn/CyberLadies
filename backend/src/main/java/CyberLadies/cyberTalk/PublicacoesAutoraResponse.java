package CyberLadies.cyberTalk;
import java.util.List;
record PublicacoesAutoraResponse(List<PublicacaoResponse> itens, int pagina, int tamanho, long total) {}
