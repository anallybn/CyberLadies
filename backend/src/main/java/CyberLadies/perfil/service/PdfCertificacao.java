package CyberLadies.perfil.service;

import CyberLadies.perfil.dto.PerfilDados.ArquivoPdf;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

final class PdfCertificacao {
    private static final long LIMITE = 5L * 1024 * 1024;

    private PdfCertificacao() {}

    static ArquivoPdf ler(MultipartFile arquivo) {
        if (arquivo.getSize() > LIMITE)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O PDF deve ter até 5 MB.");

        String recebido = arquivo.getOriginalFilename();
        if (recebido == null) throw invalido();
        String caminho = recebido.replace('\\', '/');
        String nome = caminho.substring(caminho.lastIndexOf('/') + 1)
            .replaceAll("[\\p{Cc}\\p{Cf}]", "").strip();
        if (nome.length() > 255 || nome.length() <= 4 || !nome.toLowerCase(Locale.ROOT).endsWith(".pdf"))
            throw invalido();

        byte[] dados;
        try {
            dados = arquivo.getBytes();
        } catch (IOException e) {
            throw corrompido();
        }
        if (dados.length > LIMITE)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O PDF deve ter até 5 MB.");
        if (!temCabecalhoPdf(dados)) throw invalido();
        if (!temFimPdf(dados)) throw corrompido();

        // O leitor recupera referências de PDFs legíveis quando a tabela de offsets não é estrita.
        // A leitura continua exigindo um documento com páginas e não modifica os bytes recebidos.
        boolean temPaginas;
        try (var documento = Loader.loadPDF(dados)) {
            var paginas = documento.getPages();
            int total = paginas.getCount();
            temPaginas = total > 0 && paginas.get(0) != null && paginas.get(total - 1) != null;
        } catch (InvalidPasswordException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O PDF está protegido por senha. Envie uma cópia que abra sem senha.");
        } catch (IOException | RuntimeException | StackOverflowError e) {
            throw corrompido();
        }
        if (!temPaginas) throw corrompido();
        return new ArquivoPdf(nome, dados);
    }

    private static boolean temCabecalhoPdf(byte[] dados) {
        int inicio = 0;
        if (dados.length >= 3 && dados[0] == (byte) 0xef && dados[1] == (byte) 0xbb && dados[2] == (byte) 0xbf)
            inicio = 3;
        while (inicio < Math.min(dados.length, 1024) && espacoPdf(dados[inicio])) inicio++;
        return inicio < 1024 && dados.length - inicio >= 8
            && new String(dados, inicio, 8, StandardCharsets.US_ASCII).matches("%PDF-[12]\\.[0-9]");
    }

    private static boolean temFimPdf(byte[] dados) {
        int fim = dados.length;
        while (fim > 0 && espacoPdf(dados[fim - 1])) fim--;
        return fim >= 5 && new String(dados, fim - 5, 5, StandardCharsets.US_ASCII).equals("%%EOF");
    }

    private static boolean espacoPdf(byte valor) {
        return valor == 0 || valor == 9 || valor == 10 || valor == 12 || valor == 13 || valor == 32;
    }

    private static ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo selecionado não é um PDF válido.");
    }

    private static ResponseStatusException corrompido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "O PDF está corrompido ou incompleto. Exporte o documento novamente.");
    }
}
