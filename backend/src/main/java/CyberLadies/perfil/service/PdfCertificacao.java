package CyberLadies.perfil.service;

import CyberLadies.perfil.dto.PerfilDados.ArquivoPdf;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
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

        try {
            byte[] dados = arquivo.getBytes();
            if (dados.length > LIMITE)
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O PDF deve ter até 5 MB.");
            if (dados.length < 5 || !new String(dados, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
                throw invalido();
            try (var fonte = new RandomAccessReadBuffer(dados);
                 var documento = new PDFParser(fonte).parse(false)) {
                if (documento.isEncrypted() || documento.getNumberOfPages() < 1) throw invalido();
            } catch (RuntimeException | StackOverflowError e) {
                throw invalido();
            }
            return new ArquivoPdf(nome, dados);
        } catch (IOException e) {
            throw invalido();
        }
    }

    private static ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie um arquivo PDF válido e sem senha.");
    }
}
