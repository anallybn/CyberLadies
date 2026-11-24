package CyberLadies.talk;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
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
    @Test void todosOsPapeisEditamApenasOProprioPerfil() throws Exception {
        for (String caso : new String[]{"CYBERLADIE","PENDENTE","REJEITADA","APROVADA","EMPRESA","ADMIN"}) {
            boolean educadora=caso.equals("PENDENTE") || caso.equals("REJEITADA") || caso.equals("APROVADA");
            String tipo=educadora ? "EDUCADORA" : caso;
            boolean profissional=tipo.equals("CYBERLADIE") || tipo.equals("EDUCADORA");
            long titular=db.sql("SELECT fixture_conta_integracao(?,?,'fixture','Java',?,false)")
                .params(tipo,UUID.randomUUID()+"@example.invalid",tipo).query(Long.class).single();
            if (educadora)
                db.sql("INSERT INTO educadora(conta_id,especialidade,instituicao,status) VALUES (?,'Java','Teste',?)")
                    .params(titular,caso).update();
            if (tipo.equals("EMPRESA")) {
                long empresa=db.sql("INSERT INTO cyber_empresa(nome) VALUES ('Empresa de teste') RETURNING id").query(Long.class).single();
                db.sql("INSERT INTO conta_empresa VALUES (?,?,'11222333000181','Tecnologia')").params(titular,empresa).update();
            }
            String token=sessaoPara(titular);
            String tipoPerfil=educadora && !caso.equals("APROVADA") ? "CYBERLADIE" : tipo;
            var perfil=mvc.perform(get("/api/perfil").header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pessoal.id").value(titular))
                .andExpect(jsonPath("$.pessoal.nome").value(tipo))
                .andExpect(jsonPath("$.tipo").value(tipoPerfil))
                .andExpect(jsonPath("$.dadosProfissionais").value(profissional));
            if (tipoPerfil.equals("CYBERLADIE")) perfil.andExpect(jsonPath("$.candidaturas").value(0));
            else perfil.andExpect(jsonPath("$.candidaturas").doesNotExist());
            if (tipoPerfil.equals("EDUCADORA")) perfil.andExpect(jsonPath("$.atuacaoEducadora.especialidade").value("Java"));
            else perfil.andExpect(jsonPath("$.atuacaoEducadora").doesNotExist());
            if (tipoPerfil.equals("EMPRESA")) perfil.andExpect(jsonPath("$.empresa.areaAtuacao").value("Tecnologia"))
                .andExpect(jsonPath("$.empresa.cnpj").value("11222333000181"));
            else perfil.andExpect(jsonPath("$.empresa").doesNotExist());
            var papeis=db.sql("SELECT papel FROM conta_papel WHERE conta_id=?").param(titular).query(String.class).set();
            String camposProfissionais=profissional ? ",\"formacao\":\"ADS\",\"competencias\":\"Java\"" : "";
            mvc.perform(put("/api/perfil").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Atualizado\",\"sobre\":\"Descrição\",\"id\":"+outra+
                    ",\"papeis\":[\"ADMIN\"],\"statusEducadora\":\"REJEITADA\""+camposProfissionais+"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(titular));
            mvc.perform(get("/api/perfil").header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pessoal.nome").value("Atualizado"))
                .andExpect(jsonPath("$.pessoal.sobre").value("Descrição"));
            assertThat(db.sql("SELECT nome FROM conta WHERE id=?").param(outra).query(String.class).single()).isEqualTo("Outra");
            assertThat(db.sql("SELECT papel FROM conta_papel WHERE conta_id=?").param(titular).query(String.class).set()).isEqualTo(papeis);
            if (educadora) {
                assertThat(db.sql("SELECT status FROM educadora WHERE conta_id=?").param(titular).query(String.class).single())
                    .isEqualTo(caso);
                mvc.perform(get("/api/candidaturas").header("Authorization",token))
                    .andExpect(status().is(caso.equals("APROVADA") ? 403 : 200));
            }
            mvc.perform(multipart("/api/perfil/certificacoes").file(dadosCertificacao())
                .file(pdf("certificado.pdf", gerarPdf(1))).header("Authorization",token)).andExpect(status().isCreated());
            mvc.perform(get("/api/perfil").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.certificacoes[0].nomePdf").value("certificado.pdf"));
            if (!profissional) {
                mvc.perform(put("/api/perfil").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Nome indevido\",\"sobre\":\"Descrição\",\"formacao\":\"ADS\",\"competencias\":\"Java\"}"))
                    .andExpect(status().isForbidden());
                mvc.perform(post("/api/perfil/experiencias").header("Authorization",token)
                    .contentType(MediaType.APPLICATION_JSON).content(experiencia)).andExpect(status().isForbidden());
                mvc.perform(get("/api/academy/me/progresso").header("Authorization",token)).andExpect(status().isForbidden());
            }
        }
    }
    private String sessaoPara(long conta) {
        String token="Bearer "+UUID.randomUUID().toString().replace("-","").repeat(2);
        db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour')")
            .params(CyberLadies.auth.service.Credenciais.hashBearer(token),conta).update();
        return token;
    }

    @Test void aprovacaoMudaPerfilDaMesmaSessaoSemApagarDados() throws Exception {
        mvc.perform(put("/api/perfil").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nome":"Titular","sobre":"Minha apresentação","formacao":"ADS","competencias":"Java"}
                """)).andExpect(status().isOk());
        mvc.perform(post("/api/perfil/certificacoes").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(certificado)).andExpect(status().isCreated());
        mvc.perform(post("/api/perfil/experiencias").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(experiencia)).andExpect(status().isCreated());
        mvc.perform(get("/api/perfil").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("CYBERLADIE"))
            .andExpect(jsonPath("$.candidaturas").value(0));
        mvc.perform(post("/api/educadora/solicitacao").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"especialidade":"Engenharia de software","instituicao":"Instituição de teste"}
                """)).andExpect(status().isCreated());
        mvc.perform(get("/api/perfil").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("CYBERLADIE"))
            .andExpect(jsonPath("$.atuacaoEducadora").doesNotExist());
        long empresa=db.sql("INSERT INTO cyber_empresa(nome) VALUES ('Empresa de teste') RETURNING id").query(Long.class).single();
        long vaga=db.sql("INSERT INTO cyber_vaga(empresa_id,titulo,descricao) VALUES (?,'Vaga','Descrição') RETURNING id")
            .param(empresa).query(Long.class).single();
        mvc.perform(post("/api/candidaturas").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"vagaId":%d}
                """.formatted(vaga))).andExpect(status().isCreated());
        mvc.perform(get("/api/perfil").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.candidaturas").value(1));
        long admin=db.sql("SELECT fixture_conta_integracao('Admin',?,'fixture','','ADMIN',false)")
            .param(UUID.randomUUID()+"@example.invalid").query(Long.class).single();
        mvc.perform(post("/api/admin/educadoras/solicitacoes/"+usuaria+"/decisao")
            .header("Authorization",sessaoPara(admin)).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"APROVADA"}
                """)).andExpect(status().isOk());
        mvc.perform(get("/api/perfil").header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("EDUCADORA"))
            .andExpect(jsonPath("$.atuacaoEducadora.especialidade").value("Engenharia de software"))
            .andExpect(jsonPath("$.atuacaoEducadora.instituicao").value("Instituição de teste"))
            .andExpect(jsonPath("$.pessoal.formacao").value("ADS"))
            .andExpect(jsonPath("$.pessoal.competencias").value("Java"))
            .andExpect(jsonPath("$.pessoal.sobre").value("Minha apresentação"))
            .andExpect(jsonPath("$.certificacoes.length()").value(1))
            .andExpect(jsonPath("$.experiencias.length()").value(1))
            .andExpect(jsonPath("$.candidaturas").doesNotExist());
        mvc.perform(get("/api/candidaturas").header("Authorization",auth)).andExpect(status().isForbidden());
        assertThat(db.sql("SELECT count(*) FROM cyber_candidatura WHERE usuaria_id=?").param(usuaria)
            .query(Long.class).single()).isEqualTo(1L);
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
    private MockMultipartFile dadosCertificacao() {
        return new MockMultipartFile("dados","dados.json","application/json",certificado.getBytes(StandardCharsets.UTF_8));
    }
    private MockMultipartFile pdf(String nome, byte[] bytes) {
        return new MockMultipartFile("arquivo",nome,"application/pdf",bytes);
    }
    private byte[] gerarPdf(int paginas) throws Exception {
        try (var documento=new PDDocument(); var saida=new ByteArrayOutputStream()) {
            for (int i=0;i<paginas;i++) documento.addPage(new PDPage());
            documento.save(saida);
            return saida.toByteArray();
        }
    }
    @Test void pdfPrivadoPodeSerVisualizadoBaixadoSubstituidoERemovido() throws Exception {
        byte[] original=gerarPdf(1);
        byte[] substituto=gerarPdf(2);
        mvc.perform(multipart("/api/perfil/certificacoes").file(dadosCertificacao())
            .file(pdf("Emissão.pdf",original)).header("Authorization",auth))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.nomePdf").value("Emissão.pdf"));
        long id=db.sql("SELECT id FROM perfil_certificacao WHERE usuaria_id=?").param(usuaria).query(Long.class).single();
        String rota="/api/perfil/certificacoes/"+id;
        mvc.perform(get("/api/perfil").header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.certificacoes[0].nomePdf").value("Emissão.pdf"))
            .andExpect(jsonPath("$.certificacoes[0].dados").doesNotExist());
        mvc.perform(get(rota+"/pdf").header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF)).andExpect(content().bytes(original))
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("inline;")));
        mvc.perform(get(rota+"/pdf").param("baixar","true").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(content().bytes(original))
            .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;")));
        mvc.perform(get(rota+"/pdf")).andExpect(status().isUnauthorized());
        mvc.perform(get(rota+"/pdf").header("Authorization",authOutra)).andExpect(status().isNotFound());
        mvc.perform(get(rota+"/pdf").param("baixar","true").header("Authorization",authOutra)).andExpect(status().isNotFound());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,rota).file(dadosCertificacao())
            .file(pdf("outra.pdf",substituto)).header("Authorization",authOutra)).andExpect(status().isNotFound());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,rota).file(dadosCertificacao())
            .param("removerPdf","true").header("Authorization",authOutra)).andExpect(status().isNotFound());
        mvc.perform(delete(rota).header("Authorization",authOutra)).andExpect(status().isNotFound());

        mvc.perform(put(rota).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(certificado))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nomePdf").value("Emissão.pdf"));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,rota).file(dadosCertificacao())
            .file(pdf("substituto.pdf",substituto)).header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nomePdf").value("substituto.pdf"));
        mvc.perform(get(rota+"/pdf").header("Authorization",auth)).andExpect(content().bytes(substituto));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,rota).file(dadosCertificacao())
            .param("removerPdf","true").header("Authorization",auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.nomePdf").doesNotExist());
        mvc.perform(get(rota+"/pdf").header("Authorization",auth)).andExpect(status().isNotFound());

        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,rota).file(dadosCertificacao())
            .file(pdf("novo.pdf",original)).header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(delete(rota).header("Authorization",auth)).andExpect(status().isNoContent());
        mvc.perform(get(rota+"/pdf").header("Authorization",auth)).andExpect(status().isNotFound());
        assertThat(db.sql("SELECT count(*) FROM perfil_certificacao_pdf WHERE certificacao_id=?")
            .param(id).query(Long.class).single()).isZero();
    }
    @Test void aceitaPdfComRestricaoDeEdicaoSemSenhaDeAbertura() throws Exception {
        byte[] original=pdfComProtecao("");
        mvc.perform(multipart("/api/perfil/certificacoes").file(dadosCertificacao())
            .file(pdf("restrito.pdf",original)).header("Authorization",auth))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.nomePdf").value("restrito.pdf"));
        long id=db.sql("SELECT id FROM perfil_certificacao WHERE usuaria_id=?").param(usuaria).query(Long.class).single();
        mvc.perform(get("/api/perfil/certificacoes/"+id+"/pdf").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(content().bytes(original));
    }
    @Test void aceitaPdfLegivelComOffsetDeReferenciaIncorreto() throws Exception {
        byte[] original=gerarPdf(1);
        var texto=new String(original,StandardCharsets.ISO_8859_1);
        var recuperavel=texto.replaceFirst("startxref\\s+\\d+", "startxref\n1").getBytes(StandardCharsets.ISO_8859_1);
        assertThat(recuperavel).isNotEqualTo(original);
        mvc.perform(multipart("/api/perfil/certificacoes").file(dadosCertificacao())
            .file(pdf("recuperavel.pdf",recuperavel)).header("Authorization",auth))
            .andExpect(status().isCreated());
    }
    private byte[] pdfComProtecao(String senhaAbertura) throws Exception {
        try (var documento=new PDDocument(); var saida=new ByteArrayOutputStream()) {
            documento.addPage(new PDPage());
            var permissoes=new AccessPermission();
            permissoes.setCanModify(false);
            documento.protect(new StandardProtectionPolicy("senha-proprietaria",senhaAbertura,permissoes));
            documento.save(saida);
            return saida.toByteArray();
        }
    }
    @Test void recusaPdfsInvalidosEAcimaDoLimiteSemCriarCertificacao() throws Exception {
        var rota="/api/perfil/certificacoes";
        for (var arquivo : new MockMultipartFile[]{pdf("vazio.pdf",new byte[0]),pdf("texto.pdf","texto".getBytes()),
                pdf("imagem.png",gerarPdf(1))}) {
            mvc.perform(multipart(rota).file(dadosCertificacao()).file(arquivo).header("Authorization",auth))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("O arquivo selecionado não é um PDF válido."));
        }
        for (String texto : new String[]{"%PDF-1.7\n<html>inválido</html>", "%PDF-1.7\n<html>inválido</html>\n%%EOF\n"}) {
            mvc.perform(multipart(rota).file(dadosCertificacao())
                .file(pdf("truncado.pdf",texto.getBytes(StandardCharsets.UTF_8))).header("Authorization",auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O PDF está corrompido ou incompleto. Exporte o documento novamente."));
        }
        mvc.perform(multipart(rota).file(dadosCertificacao())
            .file(pdf("protegido.pdf",pdfComProtecao("senha-de-abertura"))).header("Authorization",auth))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("O PDF está protegido por senha. Envie uma cópia que abra sem senha."));
        mvc.perform(multipart(rota).file(dadosCertificacao())
            .file(pdf("grande.pdf",new byte[5*1024*1024+1])).header("Authorization",auth))
            .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.detail").value("O PDF deve ter até 5 MB."));
        assertThat(db.sql("SELECT count(*) FROM perfil_certificacao WHERE usuaria_id=?").param(usuaria).query(Long.class).single())
            .isZero();
    }
    @Test void arquivoInvalidoNaEdicaoPreservaMetadadosEAnexo() throws Exception {
        byte[] original=gerarPdf(1);
        mvc.perform(multipart("/api/perfil/certificacoes").file(dadosCertificacao()).file(pdf("original.pdf",original))
            .header("Authorization",auth)).andExpect(status().isCreated());
        long id=db.sql("SELECT id FROM perfil_certificacao WHERE usuaria_id=?").param(usuaria).query(Long.class).single();
        String rota="/api/perfil/certificacoes/"+id;
        var novosDados=new MockMultipartFile("dados","dados.json","application/json",
            certificado.replace("Certificação teste","Nome novo").getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,rota).file(novosDados)
            .file(pdf("invalido.pdf",new byte[]{1})).header("Authorization",auth)).andExpect(status().isBadRequest());
        mvc.perform(get(rota+"/pdf").header("Authorization",auth)).andExpect(content().bytes(original));
        mvc.perform(get("/api/perfil").header("Authorization",auth))
            .andExpect(jsonPath("$.certificacoes[0].nome").value("Certificação teste"));
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
            .andExpect(jsonPath("$.candidaturas").value(0));
        mvc.perform(get("/api/perfil").header("Authorization",authOutra))
            .andExpect(jsonPath("$.participacao.publicacoes").value(0));
    }
}