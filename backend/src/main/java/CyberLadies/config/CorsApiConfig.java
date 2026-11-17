package CyberLadies.config;

import java.net.URI;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Mesmo domínio por padrão; acesso direto somente por origens HTTPS explícitas. */
@Configuration
public class CorsApiConfig implements WebMvcConfigurer {
    private final String[] origens;
    public CorsApiConfig(@Value("${APP_CORS_ORIGINS:}") String configuracao) {
        origens=Arrays.stream(configuracao.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toArray(String[]::new);
        for(String origem:origens) {
            var uri=URI.create(origem);
            if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null
                || uri.getQuery()!=null || uri.getFragment()!=null || (uri.getPath()!=null && !uri.getPath().isEmpty())
                || uri.getHost().contains("*") || uri.getHost().equals("localhost") || uri.getHost().equals("127.0.0.1"))
                throw new IllegalArgumentException("APP_CORS_ORIGINS exige origens HTTPS exatas, sem caminho ou curingas.");
        }
    }
    @Override public void addCorsMappings(CorsRegistry registry) {
        if(origens.length==0) return;
        registry.addMapping("/api/**").allowedOrigins(origens)
            .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
            .allowedHeaders("Authorization","Content-Type").exposedHeaders("Content-Disposition")
            .allowCredentials(false).maxAge(600);
    }
}
