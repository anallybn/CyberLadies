package CyberLadies.talk;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthPerfisIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    static final String SENHA="FixtureSegura123!";
    String cnpjAleatorio() {
        String base=""+(100000000000L+java.util.concurrent.ThreadLocalRandom.current().nextLong(899999999999L));
        for(int n=0;n<2;n++) {
            int soma=0,peso=base.length()-7;
            for(char d:base.toCharArray()) { soma+=(d-'0')*peso--; if(peso<2)peso=9; }
            base+=soma%11<2?0:11-soma%11;
        }
        return base;
    }
    String email() { return UUID.randomUUID()+"@example.invalid"; }
    String corpo(String perfil,String email,String cnpj) {
        String comum="{\"nome\":\"Fixture "+perfil+"\",\"email\":\""+email+"\",\"senha\":\""+SENHA+"\"";
        return comum+switch(perfil) {
            case "educadora" -> ",\"especialidade\":\"Java\",\"instituicao\":\"Instituição de teste\"}";
            case "empresa" -> ",\"cnpj\":\""+cnpj+"\",\"areaAtuacao\":\"Tecnologia\"}";
            default -> ",\"competencias\":\"Java\"}";
        };
    }
    String login(String email) throws Exception {
        String json=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\""+email+"\",\"senha\":\""+SENHA+"\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var matcher=java.util.regex.Pattern.compile("\"token\":\"([0-9a-f]{64})\"").matcher(json);
        assertThat(matcher.find()).isTrue();
        return "Bearer "+matcher.group(1);
    }
    void cadastrar(String perfil,String email,String cnpj) throws Exception {
        mvc.perform(post("/api/auth/cadastro/"+perfil).contentType(MediaType.APPLICATION_JSON).content(corpo(perfil,email,cnpj)))
            .andExpect(status().isCreated());
    }
    long id(String email) { return db.sql("SELECT id FROM talk_usuaria WHERE email=?").param(email).query(Long.class).single(); }
    @Test void tresCadastrosPersistentesELoginReconhecePerfilReal() throws Exception {
        String[] caminhos={"usuaria","educadora","empresa"},tipos={"CYBERLADIE","EDUCADORA","EMPRESA"};
        for(int i=0;i<caminhos.length;i++) {
            String email=email();
            cadastrar(caminhos[i],email,"11222333000181");
            String auth=login(email);
            mvc.perform(get("/api/auth/conta").header("Authorization",auth)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.perfil").value(tipos[i])).andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senha_hash").doesNotExist());
            assertThat(db.sql("SELECT senha_hash FROM talk_usuaria WHERE id=?").param(id(email)).query(String.class).single())
                .isNotEqualTo(SENHA);
            if(caminhos[i].equals("educadora")) {
                mvc.perform(get("/api/auth/conta").header("Authorization",auth)).andExpect(jsonPath("$.statusEducadora").value("PENDENTE"));
                assertThat(db.sql("SELECT especialidade FROM auth_educadora WHERE usuaria_id=?").param(id(email)).query(String.class).single()).isEqualTo("Java");
            }
            if(caminhos[i].equals("empresa"))
                assertThat(db.sql("SELECT count(*) FROM auth_empresa p JOIN cyber_empresa e ON e.id=p.empresa_id WHERE p.usuaria_id=?")
                    .param(id(email)).query(Long.class).single()).isEqualTo(1);
        }
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED) void emailGlobalDuplicadoEntreTiposERollbackDeCadastroEmpresarial() throws Exception {
        String email=email();
        cadastrar("usuaria",email,null);
        mvc.perform(post("/api/auth/cadastro/educadora").contentType(MediaType.APPLICATION_JSON).content(corpo("educadora",email.toUpperCase(),null)))
            .andExpect(status().isConflict());
        String cnpj=cnpjAleatorio();
        String empresa=email(); cadastrar("empresa",empresa,cnpj);
        String repetida=email();
        mvc.perform(post("/api/auth/cadastro/empresa").contentType(MediaType.APPLICATION_JSON).content(corpo("empresa",repetida,cnpj)))
            .andExpect(status().isConflict());
        assertThat(db.sql("SELECT count(*) FROM talk_usuaria WHERE email=?").param(repetida).query(Long.class).single()).isZero();
        assertThat(db.sql("SELECT count(*) FROM auth_empresa WHERE cnpj=?").param(cnpj).query(Long.class).single()).isEqualTo(1);
    }
    @Test void dadosInvalidosNaoCriamContasIncompletas() throws Exception {
        for(String perfil:new String[]{"usuaria","educadora","empresa"}) {
            String email=email();
            mvc.perform(post("/api/auth/cadastro/"+perfil).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(perfil,email,"11222333000181").replace("\"Fixture "+perfil+"\"","\"   \"")))
                .andExpect(status().isBadRequest());
            assertThat(db.sql("SELECT count(*) FROM talk_usuaria WHERE email=?").param(email).query(Long.class).single()).isZero();
        }
        mvc.perform(post("/api/auth/cadastro/educadora").contentType(MediaType.APPLICATION_JSON)
            .content(corpo("educadora",email(),null).replace("\"Java\"","\"\""))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/cadastro/empresa").contentType(MediaType.APPLICATION_JSON)
            .content(corpo("empresa",email(),"00000000000000"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/cadastro/empresa").contentType(MediaType.APPLICATION_JSON)
            .content(corpo("empresa",email(),"11222333000180"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/cadastro/empresa").contentType(MediaType.APPLICATION_JSON)
            .content(corpo("empresa",email(),"11222333000181").replace(",\"areaAtuacao\":\"Tecnologia\"",""))).andExpect(status().isBadRequest());
    }
    @Test void camposPrivilegiadosNoCadastroSaoRejeitados() throws Exception {
        for(String campo:new String[]{"tipo","perfil","status","statusAprovacao","permissoes","usuariaId","empresaId"}) {
            String email=email();
            String dados=corpo("educadora",email,null);
            dados=dados.substring(0,dados.length()-1)+",\""+campo+"\":\"APROVADA\"}";
            mvc.perform(post("/api/auth/cadastro/educadora").contentType(MediaType.APPLICATION_JSON).content(dados))
                .andExpect(status().isBadRequest());
            assertThat(db.sql("SELECT count(*) FROM talk_usuaria WHERE email=?").param(email).query(Long.class).single()).isZero();
        }
    }
    @Test void educadoraEEmpresaNaoHerdamPermissoesDaCyberLadie() throws Exception {
        for(String perfil:new String[]{"educadora","empresa"}) {
            String email=email(); cadastrar(perfil,email,"11222333000181"); String auth=login(email);
            for(String caminho:new String[]{"/api/perfil","/api/candidaturas"})
                mvc.perform(get(caminho).header("Authorization",auth)).andExpect(status().isForbidden());
            for(String caminho:new String[]{"/api/usuaria","/api/academy/me/progresso","/api/academy/me/certificados","/api/academy/me/agendamentos"})
                mvc.perform(get(caminho).header("Authorization",auth)).andExpect(status().is(perfil.equals("educadora")?200:403));
            mvc.perform(put("/api/comunidade/adesao").header("Authorization",auth)).andExpect(status().is(perfil.equals("educadora")?204:403));
            mvc.perform(post("/api/candidaturas").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":1}"))
                .andExpect(status().isForbidden());
            mvc.perform(post("/api/vagas").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
            mvc.perform(get("/api/academy/catalogo").header("Authorization",auth)).andExpect(status().isOk());
            mvc.perform(put("/api/auth/conta").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"CYBERLADIE\"}"))
                .andExpect(status().isMethodNotAllowed());
            if(perfil.equals("educadora"))
                mvc.perform(get("/api/auth/conta").header("Authorization",auth)).andExpect(jsonPath("$.statusEducadora").value("PENDENTE"));
        }
    }
    @Test void logoutRevogaSomenteASessaoApresentada() throws Exception {
        String email=email(); cadastrar("usuaria",email,null);
        String primeira=login(email),segunda=login(email);
        mvc.perform(delete("/api/auth/sessao").header("Authorization",primeira)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/conta").header("Authorization",primeira)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/conta").header("Authorization",segunda)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/conta")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\""+email+"\",\"senha\":\"incorreta\"}")).andExpect(status().isUnauthorized());
    }
    @Test void sessaoAnteriorAMigracaoContinuaReconhecida() throws Exception {
        String token="0123456789abcdef".repeat(4);
        // Esta fixture foi criada pelo ensaio ANTES da migração, na cópia restaurada.
        var legado=db.sql("SELECT usuaria_id FROM talk_sessao WHERE token_hash=?").param(ContaApi.hashBearer("Bearer "+token)).query(Long.class).optional();
        org.junit.jupiter.api.Assumptions.assumeTrue(legado.isPresent(),"Fixture de ensaio não presente neste banco.");
        mvc.perform(get("/api/auth/conta").header("Authorization","Bearer "+token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(legado.get())).andExpect(jsonPath("$.perfil").value("CYBERLADIE"));
        mvc.perform(get("/api/usuaria").header("Authorization","Bearer "+token)).andExpect(status().isOk());
    }
    @Test void tipoImutavelTambemNoPostgreSQL() throws Exception {
        String email=email(); cadastrar("usuaria",email,null);
        db.sql("SAVEPOINT auth_tipo").update();
        assertThatThrownBy(()->db.sql("UPDATE talk_usuaria SET tipo='EDUCADORA' WHERE id=?").param(id(email)).update())
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        db.sql("ROLLBACK TO SAVEPOINT auth_tipo").update();
        db.sql("RELEASE SAVEPOINT auth_tipo").update();
        assertThat(db.sql("SELECT tipo FROM talk_usuaria WHERE id=?").param(id(email)).query(String.class).single()).isEqualTo("CYBERLADIE");
    }
    @Test void origemDeValidacaoExplicitaSemPermitirOrigensArbitrarias() throws Exception {
        String email=email(); cadastrar("usuaria",email,null);
        String dados="{\"email\":\""+email+"\",\"senha\":\""+SENHA+"\"}";
        mvc.perform(post("/api/auth/login").header("Origin","http://localhost:5175").contentType(MediaType.APPLICATION_JSON).content(dados))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5175"));
        mvc.perform(post("/api/auth/login").header("Origin","https://origem-nao-autorizada.invalid").contentType(MediaType.APPLICATION_JSON).content(dados))
            .andExpect(status().isForbidden());
    }}