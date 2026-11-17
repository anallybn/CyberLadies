package CyberLadies.talk;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.test.context.jdbc.Sql(scripts="/db/fixture-conta-integracao.sql", executionPhase=org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS, config=@org.springframework.test.context.jdbc.SqlConfig(separator="@@@"))
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerfilIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    long usuaria, outra;
    String auth="Bearer "+"c".repeat(64), authOutra="Bearer "+"d".repeat(64);
    String certificado="{\"nome\":\"Certificação teste\",\"instituicao\":\"Instituição teste\",\"data\":\"2025-01-01\"}";
    String experiencia="{\"empresa\":\"Empresa teste\",\"cargo\":\"Desenvolvedora\",\"inicio\":\"2024-01-01\",\"fim\":null,\"descricao\":\"Descrição\"}";

    @BeforeEach void preparar() {
        usuaria=conta("Titular");
        outra=conta("Outra");
        for (var credencial : new Object[][]{{auth,usuaria},{authOutra,outra}})
            db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour')")
                .params(CyberLadies.auth.service.Credenciais.hashBearer((String)credencial[0]),credencial[1]).update();
    }
    long conta(String nome) {
        return db.sql("SELECT fixture_conta_integracao(?,?,'fixture','Java','CYBERLADIE',false)")
            .params(nome,UUID.randomUUID()+"@example.invalid").query(Long.class).single();
    }
    @Test void edicaoPersistidaSemAdesaoOuExposicaoDeSegredos() throws Exception {
        mvc.perform(get("/api/perfil").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.pessoal.nome").value("Titular"))
            .andExpect(jsonPath("$.pessoal.senha_hash").doesNotExist())
            .andExpect(jsonPath("$.pessoal.token").doesNotExist())
            .andExpect(jsonPath("$.certificacoes").isEmpty());
        String dados="{\"nome\":\" Nome atualizado \",\"sobre\":\" Minha trajetória \",\"formacao\":\"ADS\",\"competencias\":\"Java, React\",\"id\":"+outra+",\"email\":\"alterado@example.invalid\"}";
        mvc.perform(put("/api/perfil").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(dados))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Nome atualizado"));
        mvc.perform(get("/api/perfil").header("Authorization",auth))
            .andExpect(jsonPath("$.pessoal.sobre").value("Minha trajetória"))
            .andExpect(jsonPath("$.pessoal.formacao").value("ADS"));
        assertThat(db.sql("SELECT nome FROM conta WHERE id=?").param(outra).query(String.class).single()).isEqualTo("Outra");
        assertThat(db.sql("SELECT email FROM conta WHERE id=?").param(usuaria).query(String.class).single()).endsWith("@example.invalid");
        assertThat(db.sql("SELECT EXISTS(SELECT 1 FROM talk_participacao WHERE conta_id=?)").param(usuaria).query(Boolean.class).single()).isFalse();
    }
    @Test void crudDeCertificacoesEAutoria() throws Exception {
        mvc.perform(post("/api/perfil/certificacoes").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(certificado))
            .andExpect(status().isCreated());
        long id=db.sql("SELECT id FROM perfil_certificacao WHERE usuaria_id=?").param(usuaria).query(Long.class).single();
        mvc.perform(put("/api/perfil/certificacoes/"+id).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(certificado.replace("Certificação teste","Atualizada"))).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Atualizada"));
        mvc.perform(get("/api/perfil").header("Authorization",auth)).andExpect(jsonPath("$.certificacoes[0].nome").value("Atualizada"));
        mvc.perform(get("/api/perfil").header("Authorization",authOutra)).andExpect(jsonPath("$.certificacoes").isEmpty());
        mvc.perform(put("/api/perfil/certificacoes/"+id).header("Authorization",authOutra).contentType(MediaType.APPLICATION_JSON).content(certificado))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/api/perfil/certificacoes/"+id).header("Authorization",authOutra)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/perfil/certificacoes/"+id).header("Authorization",auth)).andExpect(status().isNoContent());
        assertThat(db.sql("SELECT count(*) FROM perfil_certificacao WHERE id=?").param(id).query(Long.class).single()).isZero();
    }
    @Test void crudDeExperienciasEAutoria() throws Exception {
        mvc.perform(post("/api/perfil/experiencias").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(experiencia))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.fim").isEmpty());
        long id=db.sql("SELECT id FROM perfil_experiencia WHERE usuaria_id=?").param(usuaria).query(Long.class).single();
        mvc.perform(put("/api/perfil/experiencias/"+id).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(experiencia.replace("Desenvolvedora","Analista").replace("null","\"2025-01-01\"")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.cargo").value("Analista"));
        mvc.perform(get("/api/perfil").header("Authorization",auth)).andExpect(jsonPath("$.experiencias[0].cargo").value("Analista"));
        mvc.perform(put("/api/perfil/experiencias/"+id).header("Authorization",authOutra).contentType(MediaType.APPLICATION_JSON).content(experiencia))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/api/perfil/experiencias/"+id).header("Authorization",authOutra)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/perfil/experiencias/"+id).header("Authorization",auth)).andExpect(status().isNoContent());
        assertThat(db.sql("SELECT count(*) FROM perfil_experiencia WHERE id=?").param(id).query(Long.class).single()).isZero();
    }
    @Test void validacoesSessaoEParametros() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"A\",\"sobre\":\"\",\"formacao\":\"\",\"competencias\":\"\"}")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/perfil").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\" \",\"sobre\":\"\",\"formacao\":\"\",\"competencias\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/perfil").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"A\",\"sobre\":\""+"a".repeat(2001)+"\",\"formacao\":\"\",\"competencias\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/perfil/certificacoes").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(certificado.replace("2025-01-01",LocalDate.now().plusYears(1).toString()))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/perfil/experiencias").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(experiencia.replace("null","\"2023-01-01\""))).andExpect(status().isBadRequest());
        db.sql("UPDATE auth_sessao SET expira_em=CURRENT_TIMESTAMP-INTERVAL '1 hour' WHERE usuaria_id=?").param(outra).update();
        mvc.perform(get("/api/perfil").header("Authorization",authOutra)).andExpect(status().isUnauthorized());
    }
    byte[] imagem(String tipo) throws Exception {
        var out=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(640,400,BufferedImage.TYPE_INT_RGB),tipo,out);
        return out.toByteArray();
    }
    @Test void fotoPrivadaValidadaRecodificadaERemovivel() throws Exception {
        for (String tipo : new String[]{"png","jpeg"}) {
            var foto=new MockMultipartFile("arquivo","foto."+tipo,"image/"+tipo,imagem(tipo));
            mvc.perform(multipart("/api/perfil/foto").file(foto).with(req->{req.setMethod("PUT"); return req;})
                .header("Authorization",auth)).andExpect(status().isNoContent());
            var bytes=mvc.perform(get("/api/perfil/foto").header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("X-Content-Type-Options","nosniff")).andReturn().getResponse().getContentAsByteArray();
            var lida=ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            assertThat(lida.getWidth()).isEqualTo(512);
            mvc.perform(get("/api/perfil").header("Authorization",auth)).andExpect(jsonPath("$.pessoal.temFoto").value(true));
        }
        mvc.perform(get("/api/perfil/foto").header("Authorization",authOutra)).andExpect(status().isNotFound());
        mvc.perform(get("/api/perfil/foto")).andExpect(status().isUnauthorized());
        var falsa=new MockMultipartFile("arquivo","foto.png","image/png","<script>teste</script>".getBytes());
        mvc.perform(multipart("/api/perfil/foto").file(falsa).with(req->{req.setMethod("PUT"); return req;})
            .header("Authorization",auth)).andExpect(status().isBadRequest());
        var grande=new MockMultipartFile("arquivo","foto.png","image/png",new byte[2*1024*1024+1]);
        mvc.perform(multipart("/api/perfil/foto").file(grande).with(req->{req.setMethod("PUT"); return req;})
            .header("Authorization",auth)).andExpect(status().isPayloadTooLarge());
        mvc.perform(delete("/api/perfil/foto").header("Authorization",auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/perfil/foto").header("Authorization",auth)).andExpect(status().isNotFound());
    }
    @Test void indicadoresVemDosRegistrosDaConta() throws Exception {
        long post=db.sql("INSERT INTO talk_post(autora_id,texto) VALUES (?,'Fixture perfil') RETURNING id").param(usuaria).query(Long.class).single();
        db.sql("INSERT INTO talk_comentario(post_id,autora_id,texto) VALUES (?,?,'Comentário')").params(post,usuaria).update();
        db.sql("INSERT INTO talk_curtida VALUES (?,?)").params(post,usuaria).update();
        mvc.perform(get("/api/perfil").header("Authorization",auth))
            .andExpect(jsonPath("$.participacao.publicacoes").value(1))
            .andExpect(jsonPath("$.participacao.comentarios").value(1))
            .andExpect(jsonPath("$.participacao.curtidas").value(1))
            .andExpect(jsonPath("$.participacao.candidaturas").value(0));
        mvc.perform(get("/api/perfil").header("Authorization",authOutra))
            .andExpect(jsonPath("$.participacao.publicacoes").value(0));
    }
}