package CyberLadies.compatibilidade;

import CyberLadies.agenda.service.EventoAgendaService;
import CyberLadies.avaliacao.service.AvaliacaoService;
import CyberLadies.certificado.service.CertificadoService;
import CyberLadies.curso.service.CursoService;
import CyberLadies.empresa.service.EmpresaService;
import CyberLadies.mentoria.service.MentoriaService;
import CyberLadies.notificacao.controller.NotificacaoController;
import CyberLadies.notificacao.service.NotificacaoService;
import CyberLadies.config.CorsApiConfig;
import CyberLadies.config.HealthApi;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contexto MVC explícito: sem Spring Boot, datasource, Flyway ou conexão de rede. */
class ProducaoWebTests {
    @Configuration
    @EnableWebMvc
    static class Web {
        @Bean EventoAgendaService agenda() { return new EventoAgendaService(); }
        @Bean AvaliacaoService avaliacao() { return new AvaliacaoService(); }
        @Bean CertificadoService certificado() { return new CertificadoService(); }
        @Bean CursoService curso() { return new CursoService(); }
        @Bean EmpresaService empresa() { return new EmpresaService(); }
        @Bean MentoriaService mentoria() { return new MentoriaService(); }
        @Bean NotificacaoService notificacao() { return new NotificacaoService(); }
    }

    private AnnotationConfigWebApplicationContext contexto(String perfil) {
        var contexto = new AnnotationConfigWebApplicationContext();
        contexto.setServletContext(new MockServletContext());
        contexto.getEnvironment().setActiveProfiles(perfil);
        contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("teste",
            Map.of("APP_CORS_ORIGINS", "https://cyber-ladies.vercel.app")));
        contexto.register(Web.class, CorsApiConfig.class, HealthApi.class, NotificacaoController.class);
        ModulosMainTests.contratos().forEach(c -> contexto.register(c.controller().getClass()));
        contexto.refresh();
        return contexto;
    }

    @Test void producaoNaoRegistraNenhumDosSeteControllersLegados() throws Exception {
        try (var contexto = contexto("production")) {
            var mvc = webAppContextSetup(contexto).build();
            for (var contrato : ModulosMainTests.contratos().toList()) {
                assertTrue(contexto.getBeansOfType(contrato.controller().getClass()).isEmpty());
                mvc.perform(get("/api/" + contrato.rota())).andExpect(status().isNotFound());
                mvc.perform(post("/api/" + contrato.rota())).andExpect(status().isNotFound());
                mvc.perform(put("/api/" + contrato.rota() + "/1")).andExpect(status().isNotFound());
                mvc.perform(delete("/api/" + contrato.rota() + "/1")).andExpect(status().isNotFound());
            }
            assertTrue(contexto.getBeansOfType(NotificacaoController.class).isEmpty());
            mvc.perform(post("/api/notificacoes/verificar-leitura")).andExpect(status().isNotFound());
        }
    }

    @Test void ambienteLocalMantemRotasLegadas() throws Exception {
        try (var contexto = contexto("local")) {
            var mvc = webAppContextSetup(contexto).build();
            for (var contrato : ModulosMainTests.contratos().toList()) {
                mvc.perform(get("/api/" + contrato.rota())).andExpect(status().isOk());
            }
            assertEquals(1, contexto.getBeansOfType(NotificacaoController.class).size());
        }
    }

    @Test void healthPublicoNaoExigeBancoNemExpoeDetalhes() throws Exception {
        try (var contexto = contexto("production")) {
            webAppContextSetup(contexto).build().perform(get("/api/health"))
                .andExpect(status().isOk()).andExpect(content().json("{\"status\":\"UP\"}"));
        }
    }

    @Test void corsAceitaIdempotenciaSomenteNaOrigemAutorizada() throws Exception {
        try (var contexto = contexto("production")) {
            var mvc = webAppContextSetup(contexto).build();
            mvc.perform(options("/api/health").header("Origin", "https://cyber-ladies.vercel.app")
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "authorization,content-type,idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://cyber-ladies.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Headers", "authorization, content-type, idempotency-key"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
            for (String origem : new String[]{"https://outra.example.invalid", "http://cyber-ladies.vercel.app"}) {
                mvc.perform(options("/api/health").header("Origin", origem)
                    .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
            }
        }
    }

    @Test void producaoExigeAsTresVariaveisDoBancoSemFallbackLocal() throws Exception {
        var propriedades = new Properties();
        try (var entrada = getClass().getResourceAsStream("/application-production.properties")) {
            assertNotNull(entrada);
            propriedades.load(entrada);
        }
        var ambiente = new StandardEnvironment();
        ambiente.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        ambiente.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        for (String campo : new String[]{"url", "username", "password"}) {
            var valor = propriedades.getProperty("spring.datasource." + campo);
            assertThrows(IllegalArgumentException.class, () -> ambiente.resolveRequiredPlaceholders(valor));
        }
        assertEquals("https://cyber-ladies.vercel.app", propriedades.getProperty("APP_CORS_ORIGINS"));
    }
}
