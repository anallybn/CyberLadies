package CyberLadies.perfil.service;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FotoPerfil {
    public byte[] normalizar(MultipartFile arquivo) {
        if (arquivo.isEmpty()) throw invalida("Escolha uma imagem PNG ou JPEG.");
        if (arquivo.getSize()>2*1024*1024)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"A foto deve ter até 2 MB.");
        if (!Set.of("image/png","image/jpeg").contains(String.valueOf(arquivo.getContentType())))
            throw invalida("Envie somente PNG ou JPEG.");
        try (var input=ImageIO.createImageInputStream(new ByteArrayInputStream(arquivo.getBytes()))) {
            var leitores=ImageIO.getImageReaders(input);
            if (!leitores.hasNext()) throw invalida("O arquivo não é uma imagem válida.");
            var leitor=leitores.next();
            try {
                if (!Set.of("png","jpeg").contains(leitor.getFormatName().toLowerCase(java.util.Locale.ROOT)))
                    throw invalida("Envie somente PNG ou JPEG.");
                leitor.setInput(input,true,true);
                int largura=leitor.getWidth(0),altura=leitor.getHeight(0);
                if (largura<1 || altura<1 || largura>4096 || altura>4096 || (long)largura*altura>12_000_000)
                    throw invalida("A imagem deve ter até 4096 px por lado e 12 milhões de pixels.");
                BufferedImage original=leitor.read(0);
                double escala=Math.min(1,512.0/Math.max(largura,altura));
                var limpa=new BufferedImage(Math.max(1,(int)(largura*escala)),Math.max(1,(int)(altura*escala)),BufferedImage.TYPE_INT_ARGB);
                var grafico=limpa.createGraphics();
                try {
                    grafico.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    grafico.drawImage(original,0,0,limpa.getWidth(),limpa.getHeight(),null);
                } finally { grafico.dispose(); }
                var saida=new ByteArrayOutputStream();
                if (!ImageIO.write(limpa,"png",saida)) throw invalida("Não foi possível processar a imagem.");
                // Recodifica somente pixels: remove EXIF, nomes de arquivo e metadados.
                return saida.toByteArray();
            } finally { leitor.dispose(); }
        } catch (IOException | IllegalArgumentException e) {
            throw invalida("Não foi possível ler a imagem. Escolha outro PNG ou JPEG.");
        }
    }
    private ResponseStatusException invalida(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensagem);
    }
}