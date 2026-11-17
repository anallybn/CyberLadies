package CyberLadies.talk;
import CyberLadies.config.CorsApiConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="APP_CORS_ORIGINS=https://frontend.example.invalid")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="DATABASE_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class CorsApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void permiteSomenteOrigemConfiguradaSemLiberarAutenticacao() throws Exception {
        mvc.perform(options("/api/posts").header("Origin","https://frontend.example.invalid")
            .header("Access-Control-Request-Method","PUT").header("Access-Control-Request-Headers","authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","https://frontend.example.invalid"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(get("/api/posts").header("Origin","https://frontend.example.invalid"))
            .andExpect(status().isUnauthorized());
        mvc.perform(options("/api/posts").header("Origin","https://outro.example.invalid").header("Access-Control-Request-Method","PUT"))
            .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void recusaCuringasCaminhosEOrigensInseguras() {
        for(String origem:new String[]{"*","http://site.example.invalid","https://site.example.invalid/caminho","https://localhost"})
            assertThrows(IllegalArgumentException.class,() -> new CorsApiConfig(origem));
    }
}
