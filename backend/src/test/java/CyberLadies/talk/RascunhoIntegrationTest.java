package CyberLadies.talk;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class RascunhoIntegrationTest {
    @Autowired MockMvc mvc; @Autowired JdbcClient db;
    String caminho="/api/educadora/materiais/rascunhos";
    String auth="Bearer "+"7".repeat(64),outraAuth="Bearer "+"8".repeat(64),alunaAuth="Bearer "+"9".repeat(64),empresaAuth="Bearer "+"a".repeat(64);
    long dona,outra,trilha,alheia;
    @BeforeEach void preparar() {
        assertThat(db.sql("SELECT current_database()").query(String.class).single()).isEqualTo("cyberladies_auth_restore_5958ab52de");
        dona=conta("EDUCADORA",auth);outra=conta("EDUCADORA",outraAuth);conta("CYBERLADIE",alunaAuth);conta("EMPRESA",empresaAuth);
        trilha=trilha(dona);alheia=trilha(outra);
    }
    long conta(String tipo,String auth) {
        long id=db.sql("INSERT INTO talk_usuaria(nome,email,senha_hash,competencias,tipo) VALUES ('Fixture rascunhos',?,'fixture','',?) RETURNING id")
            .params(UUID.randomUUID()+"@example.invalid",tipo).query(Long.class).single();
        if(tipo.equals("EDUCADORA"))db.sql("INSERT INTO auth_educadora(usuaria_id,especialidade,instituicao) VALUES (?,'Teste','Teste')").param(id).update();
        db.sql("INSERT INTO talk_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour')").params(ContaApi.hashBearer(auth),id).update();
        return id;
    }
    long trilha(long autora) {
        long id=db.sql("INSERT INTO academy_trilha(titulo,descricao,categoria,nivel) VALUES ('Fixture privada','Teste','DADOS','INICIANTE') RETURNING id").query(Long.class).single();
        db.sql("INSERT INTO academy_autoria(trilha_id,educadora_id) VALUES (?,?)").params(id,autora).update();return id;
    }
    String criar(String json,String chave) throws Exception {
        return mvc.perform(post(caminho).header("Authorization",auth).header("Idempotency-Key",chave).contentType("application/json").content(json))
            .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsString();
    }
    String id(String json) { var m=java.util.regex.Pattern.compile("\"id\":\"([^\"]+)\"").matcher(json);assertThat(m.find()).isTrue();return m.group(1); }
    @Test void pendenteCriaListaRecuperaEEditaConteudoIncompleto() throws Exception {
        String uuid=id(criar("{\"titulo\":\"Meu rascunho\"}",UUID.randomUUID().toString()));
        mvc.perform(get(caminho+"/"+uuid).header("Authorization",auth)).andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Meu rascunho"))
            .andExpect(jsonPath("$.revisao").value(1)).andExpect(jsonPath("$.videoId").isEmpty());
        mvc.perform(put(caminho+"/"+uuid).header("Authorization",auth).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
            .content("{\"titulo\":\"Editado\",\"descricao\":\"Continuação\",\"revisao\":1,\"tags\":[\"Java\",\"java\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.revisao").value(2)).andExpect(jsonPath("$.tags.length()").value(1));
        mvc.perform(get(caminho).header("Authorization",auth)).andExpect(jsonPath("$[0].titulo").value("Editado"))
            .andExpect(jsonPath("$[0].descricao").doesNotExist()).andExpect(jsonPath("$[0].videoId").doesNotExist());
        assertThat(db.sql("SELECT status FROM auth_educadora WHERE usuaria_id=?").param(dona).query(String.class).single()).isEqualTo("PENDENTE");
    }
    @Test void isolamentoEPerfisBloqueados() throws Exception {
        String uuid=id(criar("{\"titulo\":\"Privado\"}",UUID.randomUUID().toString()));
        mvc.perform(get(caminho).header("Authorization",outraAuth).param("educadoraId",""+dona)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get(caminho+"/"+uuid).header("Authorization",outraAuth)).andExpect(status().isNotFound());
        mvc.perform(put(caminho+"/"+uuid).header("Authorization",outraAuth).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
            .content("{\"titulo\":\"Ataque\",\"revisao\":1}")).andExpect(status().isNotFound());
        for(String token:List.of(alunaAuth,empresaAuth)) {
            mvc.perform(get(caminho).header("Authorization",token)).andExpect(status().isForbidden());
            mvc.perform(post(caminho).header("Authorization",token).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content("{\"titulo\":\"Ataque\"}"))
                .andExpect(status().isForbidden());
            mvc.perform(get(caminho+"/"+uuid).header("Authorization",token)).andExpect(status().isForbidden());
        }
        mvc.perform(get(caminho)).andExpect(status().isUnauthorized());
    }
    @Test void associacaoPropriaNoServicoENoBanco() throws Exception {
        String uuid=id(criar("{\"titulo\":\"Próprio\",\"trilhaId\":"+trilha+"}",UUID.randomUUID().toString()));
        mvc.perform(post(caminho).header("Authorization",auth).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
            .content("{\"titulo\":\"Alheio\",\"trilhaId\":"+alheia+"}")).andExpect(status().isBadRequest());
        rejeitar("UPDATE academy_material_rascunho SET trilha_id=?,revisao=revisao+1 WHERE id=?",alheia,UUID.fromString(uuid));
        rejeitar("UPDATE academy_material_rascunho SET educadora_id=?,revisao=revisao+1 WHERE id=?",outra,UUID.fromString(uuid));
    }
    void rejeitar(String sql,Object...args) {
        db.sql("SAVEPOINT rascunho_rejeicao").update();
        assertThatThrownBy(()->db.sql(sql).params(args).update()).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        db.sql("ROLLBACK TO SAVEPOINT rascunho_rejeicao").update();
    }
    @Test void contratoEstritoELimitesValidamSemPublicacao() throws Exception {
        for(String json:List.of("{}","{\"titulo\":\" \"}","{\"titulo\":\"X\",\"duracaoMinutos\":0}","{\"titulo\":\"X\",\"tipo\":\"PDF\"}",
            "{\"titulo\":\"X\",\"videoProvedor\":\"OUTRO\"}","{\"titulo\":\"X\",\"videoProvedor\":\"YOUTUBE\",\"videoId\":\"<iframe>\"}",
            "{\"titulo\":\"X\",\"educadoraId\":1}","{\"titulo\":\"X\",\"status\":\"APROVADA\"}","{\"titulo\":\"X\",\"permissoes\":[]}",
            "{\"titulo\":\"X\",\"tags\":[\"\" ]}")) {
            mvc.perform(post(caminho).header("Authorization",auth).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(json))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/educadora/publicacoes").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(post(caminho+"/00000000-0000-0000-0000-000000000000/publicar").header("Authorization",auth)).andExpect(status().is4xxClientError());
    }
    @Test void criacaoIdempotenteEAtualizacaoRepetidaNaoAvancamRevisao() throws Exception {
        String chave=UUID.randomUUID().toString(),body="{\"titulo\":\"Único\"}";
        String uuid=id(criar(body,chave));assertThat(id(criar(body,chave))).isEqualTo(uuid);
        mvc.perform(post(caminho).header("Authorization",auth).header("Idempotency-Key",chave).contentType("application/json").content("{\"titulo\":\"Diferente\"}"))
            .andExpect(status().isConflict());
        String op=UUID.randomUUID().toString(),update="{\"titulo\":\"Mudança\",\"revisao\":1}";
        for(int i=0;i<2;i++)mvc.perform(put(caminho+"/"+uuid).header("Authorization",auth).header("Idempotency-Key",op).contentType("application/json").content(update))
            .andExpect(status().isOk()).andExpect(jsonPath("$.revisao").value(2));
        assertThat(db.sql("SELECT count(*) FROM academy_material_rascunho WHERE educadora_id=?").param(dona).query(Long.class).single()).isEqualTo(1);
    }
    @Test void revisaoAntigaRetornaConflitoENaoSobrescreve() throws Exception {
        String uuid=id(criar("{\"titulo\":\"Original\"}",UUID.randomUUID().toString()));
        mvc.perform(put(caminho+"/"+uuid).header("Authorization",auth).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
            .content("{\"titulo\":\"Aba A\",\"revisao\":1}")).andExpect(status().isOk());
        mvc.perform(put(caminho+"/"+uuid).header("Authorization",auth).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
            .content("{\"titulo\":\"Aba B\",\"revisao\":1}")).andExpect(status().isConflict());
        mvc.perform(get(caminho+"/"+uuid).header("Authorization",auth)).andExpect(jsonPath("$.titulo").value("Aba A"));
    }
    @Test void rascunhoNaoEntraNoCatalogoNemMudaConteudosOuCertificados() throws Exception {
        long aulas=db.sql("SELECT count(*) FROM academy_aula").query(Long.class).single();
        long progresso=db.sql("SELECT count(*) FROM academy_conclusao").query(Long.class).single();
        long certificados=db.sql("SELECT count(*) FROM academy_certificado").query(Long.class).single();
        String marcador="privado-"+UUID.randomUUID();
        criar("{\"titulo\":\""+marcador+"\",\"trilhaId\":"+trilha+"}",UUID.randomUUID().toString());
        mvc.perform(get("/api/academy/catalogo").param("busca",marcador)).andExpect(jsonPath("$.aulas").isEmpty()).andExpect(jsonPath("$.trilhas").isEmpty());
        assertThat(db.sql("SELECT count(*) FROM academy_aula").query(Long.class).single()).isEqualTo(aulas);
        assertThat(db.sql("SELECT count(*) FROM academy_conclusao").query(Long.class).single()).isEqualTo(progresso);
        assertThat(db.sql("SELECT count(*) FROM academy_certificado").query(Long.class).single()).isEqualTo(certificados);
    }
}
