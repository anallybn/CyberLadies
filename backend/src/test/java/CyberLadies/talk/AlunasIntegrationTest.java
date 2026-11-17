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

@org.springframework.test.context.jdbc.Sql(scripts="/db/fixture-conta-integracao.sql", executionPhase=org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS, config=@org.springframework.test.context.jdbc.SqlConfig(separator="@@@"))
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="DATABASE_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
@SpringBootTest @AutoConfigureMockMvc @Transactional
class AlunasIntegrationTest {
    @Autowired MockMvc mvc; @Autowired JdbcClient db;
    String base="/api/educadora/alunas",auth="Bearer "+"b".repeat(64),outroAuth="Bearer "+"c".repeat(64),pendenteAuth="Bearer "+"d".repeat(64),alunaAuth="Bearer "+"e".repeat(64),empresaAuth="Bearer "+"f".repeat(64);
    long dona,outra,pendente,ana,bia,semProgresso,trilha,extra,alheia,aula1,aula2,extra1,extra2;
    @BeforeEach void preparar(){
        assertThat(db.sql("SELECT current_database()").query(String.class).single()).isEqualTo("cyberladies_auth_test");
        dona=conta("EDUCADORA",auth,true);outra=conta("EDUCADORA",outroAuth,true);pendente=conta("EDUCADORA",pendenteAuth,false);
        ana=conta("CYBERLADIE",alunaAuth,false);bia=conta("CYBERLADIE",null,false);semProgresso=conta("CYBERLADIE",null,false);conta("EMPRESA",empresaAuth,false);
        db.sql("UPDATE conta SET nome='Ana Teste' WHERE id=?").param(ana).update();
        db.sql("UPDATE conta SET nome='Bia Teste' WHERE id=?").param(bia).update();
        db.sql("UPDATE conta SET nome='Sem Progresso' WHERE id=?").param(semProgresso).update();
        trilha=trilha(dona);extra=trilha(dona);alheia=trilha(outra);
        aula1=aula(trilha,1);aula2=aula(trilha,2);extra1=aula(extra,1);extra2=aula(extra,2);aula(alheia,1);
        inscrever(ana,trilha);inscrever(bia,trilha);inscrever(semProgresso,trilha);inscrever(ana,extra);inscrever(bia,alheia);
        concluir(ana,trilha,aula1);concluir(ana,trilha,aula2);concluir(bia,trilha,aula1);concluir(ana,extra,extra1);
    }
    long conta(String tipo,String token,boolean aprovada){
        long id=db.sql("SELECT fixture_conta_integracao('Fixture alunas',?,'fixture','privado',?,false)").params(UUID.randomUUID()+"@example.invalid",tipo).query(Long.class).single();
        if(tipo.equals("EDUCADORA")){
            db.sql("INSERT INTO educadora(conta_id,especialidade,instituicao) VALUES (?,'Teste','Teste')").param(id).update();
            if(aprovada)db.sql("UPDATE educadora SET status='APROVADA' WHERE conta_id=?").param(id).update();
        }
        if(token!=null)db.sql("INSERT INTO auth_sessao VALUES (?,?,CURRENT_TIMESTAMP+INTERVAL '1 hour')").params(CyberLadies.auth.service.Credenciais.hashBearer(token),id).update();
        return id;
    }
    long trilha(long autora){
        long id=db.sql("INSERT INTO academy_trilha(titulo,descricao,categoria,nivel) VALUES ('Fixture acompanhamento','Teste','DADOS','INICIANTE') RETURNING id").query(Long.class).single();
        db.sql("INSERT INTO academy_autoria(trilha_id,educadora_id) VALUES (?,?)").params(id,autora).update();return id;
    }
    long aula(long trilha,int ordem){
        return db.sql("INSERT INTO academy_aula(trilha_id,titulo,descricao,ordem,duracao_minutos,video_provedor,video_id,uso_autorizado) VALUES (?,'Aula fixture','Teste',?,10,'YOUTUBE','M7lc1UVf-VE',true) RETURNING id").params(trilha,ordem).query(Long.class).single();
    }
    void inscrever(long pessoa,long trilha){db.sql("INSERT INTO academy_inscricao(usuaria_id,trilha_id) VALUES (?,?)").params(pessoa,trilha).update();}
    void concluir(long pessoa,long trilha,long aula){db.sql("INSERT INTO academy_conclusao(usuaria_id,trilha_id,aula_id) VALUES (?,?,?)").params(pessoa,trilha,aula).update();}
    String detalhe(long pessoa,long trilha){return base+"/"+pessoa+"/trilhas/"+trilha+"/progresso";}
    @Test void indicadoresDistintosEProgressoZeroParcialIntegral()throws Exception{
        mvc.perform(get(base).header("Authorization",auth)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.indicadores.totalAlunas").value(3)).andExpect(jsonPath("$.indicadores.inscricoesConcluidas").value(1))
            .andExpect(jsonPath("$.indicadores.inscricoesEmAndamento").value(3)).andExpect(jsonPath("$.indicadores.inscricoes").value(4))
            .andExpect(jsonPath("$.inscricoes.length()").value(4));
        for(var caso:List.of(new long[]{ana,trilha,100},new long[]{bia,trilha,50},new long[]{semProgresso,trilha,0},new long[]{ana,extra,50})){
            mvc.perform(get(detalhe(caso[0],caso[1])).header("Authorization",auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.inscricao.percentual").value((int)caso[2])).andExpect(jsonPath("$.aulas.length()").value(2));
        }
    }
    @Test void pesquisaFiltrosEIndicadoresDoConjuntoCompleto()throws Exception{
        mvc.perform(get(base).header("Authorization",auth).param("busca","aNa")).andExpect(jsonPath("$.inscricoes.length()").value(2))
            .andExpect(jsonPath("$.indicadores.totalAlunas").value(3));
        mvc.perform(get(base).header("Authorization",auth).param("status","CONCLUIDAS")).andExpect(jsonPath("$.inscricoes.length()").value(1));
        mvc.perform(get(base).header("Authorization",auth).param("status","ANDAMENTO")).andExpect(jsonPath("$.inscricoes.length()").value(3));
        mvc.perform(get(base).header("Authorization",auth).param("trilhaId",""+extra)).andExpect(jsonPath("$.inscricoes.length()").value(1));
        mvc.perform(get(base).header("Authorization",auth).param("busca","ninguém")).andExpect(jsonPath("$.inscricoes").isEmpty());
        mvc.perform(get(base).header("Authorization",auth).param("trilhaId",""+alheia)).andExpect(status().isNotFound());
        mvc.perform(get(base).header("Authorization",auth).param("status","INATIVAS")).andExpect(status().isBadRequest());
        mvc.perform(get(base).header("Authorization",auth).param("busca","x".repeat(161))).andExpect(status().isBadRequest());
    }
    @Test void isolamentoDaListaEDosDetalhes()throws Exception{
        mvc.perform(get(base).header("Authorization",outroAuth).param("educadoraId",""+dona)).andExpect(jsonPath("$.indicadores.inscricoes").value(1))
            .andExpect(jsonPath("$.inscricoes[0].trilhaId").value(alheia));
        mvc.perform(get(detalhe(ana,trilha)).header("Authorization",outroAuth)).andExpect(status().isNotFound());
        mvc.perform(get(detalhe(bia,alheia)).header("Authorization",auth)).andExpect(status().isNotFound());
        mvc.perform(get(detalhe(semProgresso,extra)).header("Authorization",auth)).andExpect(status().isNotFound());
    }
    @Test void pendenteEOutrosPerfisNaoAcessamNenhumEndpoint()throws Exception{
        for(String token:List.of(pendenteAuth,alunaAuth,empresaAuth)){
            mvc.perform(get(base).header("Authorization",token)).andExpect(status().isForbidden());
            mvc.perform(get(detalhe(ana,trilha)).header("Authorization",token)).andExpect(status().isForbidden());
        }
        mvc.perform(get(base)).andExpect(status().isUnauthorized());
        mvc.perform(get(detalhe(ana,trilha))).andExpect(status().isUnauthorized());
        db.sql("UPDATE educadora SET status='PENDENTE' WHERE conta_id=?").param(dona).update();
        mvc.perform(get(detalhe(ana,trilha)).header("Authorization",auth)).andExpect(status().isForbidden());
        // Acesso privado de pendentes ainda não foi autorizado nesta integração.
        mvc.perform(get("/api/educadora/dashboard").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/educadora/materiais/rascunhos").header("Authorization",auth)).andExpect(status().isForbidden());
    }
    @Test void dadosMinimosSemPerfilPrivadoOuMetadadosDoVideo()throws Exception{
        String lista=mvc.perform(get(base).header("Authorization",auth)).andReturn().getResponse().getContentAsString();
        String item=mvc.perform(get(detalhe(ana,trilha)).header("Authorization",auth)).andReturn().getResponse().getContentAsString();
        for(String json:List.of(lista,item))assertThat(json).doesNotContain("email","telefone","senha","competencias","videoId","videoProvedor","conteudoComplementar","privado");
        mvc.perform(get("/api/perfil").header("Authorization",auth)).andExpect(status().isForbidden());
        mvc.perform(get(detalhe(ana,trilha)).header("Authorization",auth)).andExpect(jsonPath("$.aulas[0].concluida").value(true))
            .andExpect(jsonPath("$.inscricao.inscritaEm").exists());
    }
    @Test void estadosSemTrilhasESemInscricoes()throws Exception{
        String vazioAuth="Bearer "+"0".repeat(64);long vazia=conta("EDUCADORA",vazioAuth,true);
        mvc.perform(get(base).header("Authorization",vazioAuth)).andExpect(jsonPath("$.trilhas").isEmpty()).andExpect(jsonPath("$.inscricoes").isEmpty())
            .andExpect(jsonPath("$.indicadores.totalAlunas").value(0));
        trilha(vazia);
        mvc.perform(get(base).header("Authorization",vazioAuth)).andExpect(jsonPath("$.trilhas.length()").value(1)).andExpect(jsonPath("$.inscricoes").isEmpty());
    }
}
