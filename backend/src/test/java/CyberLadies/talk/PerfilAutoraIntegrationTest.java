package CyberLadies.talk;

import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
@Sql(scripts="/db/fixture-conta-integracao.sql", executionPhase=Sql.ExecutionPhase.BEFORE_TEST_CLASS, config=@SqlConfig(separator="@@@"))
class PerfilAutoraIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    long leitora, autora, post;
    String auth="Bearer "+"7".repeat(64), authAutora="Bearer "+"8".repeat(64);

    @BeforeEach void preparar() {
        // Nomes iguais não identificam contas; conteúdo privado é deliberadamente preenchido.
        leitora=conta(auth); autora=conta(authAutora);
        post=publicar(autora,"Publicação da autora");
        db.sql("UPDATE conta SET sobre='Apresentação privada',formacao='Formação privada' WHERE id=?").param(autora).update();
        db.sql("INSERT INTO perfil_foto VALUES (?,?)").params(autora,new byte[]{1,2,3}).update();
    }
    long conta(String bearer) {
        long id=db.sql("SELECT fixture_conta_integracao('Mesmo nome',?,'fixture','Competências privadas','CYBERLADIE',true)")
            .param(UUID.randomUUID()+"@example.invalid").query(Long.class).single();
        db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour')")
            .params(CyberLadies.auth.service.Credenciais.hashBearer(bearer),id).update();
        return id;
    }
    long publicar(long id,String texto) {
        return db.sql("INSERT INTO talk_post(autora_id,texto,criado_em) VALUES (?,?,'2026-11-16T12:00:00Z') RETURNING id")
            .params(id,texto).query(Long.class).single();
    }
    String perfil() { return "/api/comunidade/perfis/"+autora; }

    @Test void projecaoMinimaNaoVazaPerfilPrivado() throws Exception {
        for (String sessao : new String[]{auth,authAutora}) {
            mvc.perform(get(perfil()).header("Authorization",sessao))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(content().json("{\"id\":"+autora+",\"nome\":\"Mesmo nome\"}",JsonCompareMode.STRICT));
        }
        mvc.perform(get("/api/posts/"+post).header("Authorization",auth))
            .andExpect(jsonPath("$.autoraId").value(autora)).andExpect(jsonPath("$.propria").value(false));
        mvc.perform(get("/api/perfil").header("Authorization",authAutora))
            .andExpect(jsonPath("$.pessoal.sobre").value("Apresentação privada"));
    }
    @Test void paginasFiltramPorIdEReutilizamAutoriaDoFeed() throws Exception {
        publicar(leitora,"Não deve aparecer");
        long ultimo=0;
        for(int i=0;i<11;i++) ultimo=publicar(autora,"Texto "+i);
        mvc.perform(get(perfil()+"/publicacoes").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.total").value(12)).andExpect(jsonPath("$.itens.length()").value(10))
            .andExpect(jsonPath("$.itens[0].id").value(ultimo))
            .andExpect(jsonPath("$.itens[0].autoraId").value(autora)).andExpect(jsonPath("$.itens[0].propria").value(false));
        mvc.perform(get(perfil()+"/publicacoes?pagina=1").header("Authorization",authAutora))
            .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(2))
            .andExpect(jsonPath("$.itens[1].id").value(post)).andExpect(jsonPath("$.itens[1].propria").value(true));
        mvc.perform(get(perfil()+"/publicacoes?pagina=2").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.itens").isEmpty());
    }
    @Test void exigeSessaoEParticipacaoEmTodosOsEndpoints() throws Exception {
        for(String rota : new String[]{perfil(),perfil()+"/publicacoes"})
            mvc.perform(get(rota)).andExpect(status().isUnauthorized());
        db.sql("DELETE FROM talk_participacao WHERE conta_id=?").param(leitora).update();
        for(String rota : new String[]{perfil(),perfil()+"/publicacoes"})
            mvc.perform(get(rota).header("Authorization",auth)).andExpect(status().isForbidden());
    }
    @Test void ausentesSemPublicacoesEParametrosInvalidos() throws Exception {
        for(String sufixo : new String[]{"","/publicacoes"}) {
            mvc.perform(get("/api/comunidade/perfis/9223372036854775807"+sufixo).header("Authorization",auth)).andExpect(status().isNotFound());
            mvc.perform(get("/api/comunidade/perfis/"+leitora+sufixo).header("Authorization",auth)).andExpect(status().isNotFound());
        }
        for(String query : new String[]{"pagina=-1","tamanho=0","tamanho=51","pagina=abc"})
            mvc.perform(get(perfil()+"/publicacoes?"+query).header("Authorization",auth)).andExpect(status().isBadRequest());
    }
    @Test void naoPermiteEditarPerfilOuPublicacaoAlheia() throws Exception {
        mvc.perform(put(perfil()).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/posts/"+post).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Invasão\"}"))
            .andExpect(status().isForbidden());
        // O contrato privado continua vinculado à sessão, mesmo com ID alheio no corpo.
        mvc.perform(put("/api/perfil").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":"+autora+",\"nome\":\"Leitora editada\",\"sobre\":\"\",\"formacao\":\"\",\"competencias\":\"\"}"))
            .andExpect(status().isOk());
        assertThat(db.sql("SELECT nome FROM conta WHERE id=?").param(autora).query(String.class).single()).isEqualTo("Mesmo nome");
        assertThat(db.sql("SELECT texto FROM talk_post WHERE id=?").param(post).query(String.class).single()).isEqualTo("Publicação da autora");
        mvc.perform(get(perfil()+"/foto").header("Authorization",auth)).andExpect(status().isNotFound());
    }
}
