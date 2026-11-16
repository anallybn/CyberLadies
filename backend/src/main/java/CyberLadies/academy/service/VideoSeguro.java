package CyberLadies.academy.service;

import CyberLadies.academy.dto.AcademyDados.Video;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class VideoSeguro {
    public Video gerar(String provedor,String id) {
        if ("YOUTUBE".equals(provedor) && id!=null && id.matches("[A-Za-z0-9_-]{11}"))
            return new Video(provedor,"https://www.youtube-nocookie.com/embed/"+id);
        if ("VIMEO".equals(provedor) && id!=null && id.matches("[1-9][0-9]{0,11}"))
            return new Video(provedor,"https://player.vimeo.com/video/"+id);
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Vídeo não possui um provedor ou identificador autorizado.");
    }
}