package CyberLadies.compatibilidade;

import CyberLadies.agenda.controller.EventoAgendaController;
import CyberLadies.agenda.service.EventoAgendaService;
import CyberLadies.avaliacao.controller.AvaliacaoController;
import CyberLadies.avaliacao.service.AvaliacaoService;


import CyberLadies.certificado.controller.CertificadoController;
import CyberLadies.certificado.service.CertificadoService;
import CyberLadies.curso.controller.CursoController;
import CyberLadies.curso.service.CursoService;
import CyberLadies.empresa.controller.EmpresaController;
import CyberLadies.empresa.service.EmpresaService;
import CyberLadies.mentoria.controller.MentoriaController;
import CyberLadies.mentoria.service.MentoriaService;
import CyberLadies.notificacao.controller.NotificacaoController;
import CyberLadies.notificacao.service.NotificacaoService;


import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Controllers e serviços reais, sem datasource ou acesso a qualquer banco.
class ModulosMainTests {
    record Contrato(String rota, Object controller, String dados, String campo) {
        public String toString() { return rota; }
    }

    static Stream<Contrato> contratos() {
        return Stream.of(
            new Contrato("agenda", new EventoAgendaController(new EventoAgendaService()),
                "{\"usuariaId\":1,\"titulo\":\"Original\",\"tipoEvento\":\"CURSO\",\"dataInicio\":\"2030-01-01T10:00:00\",\"dataFim\":\"2030-01-01T11:00:00\"}", "titulo"),
            new Contrato("avaliacoes", new AvaliacaoController(new AvaliacaoService()),
                "{\"usuariaId\":1,\"tipoAtividade\":\"CURSO\",\"atividadeId\":1,\"nota\":5,\"comentario\":\"Original\"}", "comentario"),
            new Contrato("certificados", new CertificadoController(new CertificadoService()),
                "{\"usuariaId\":1,\"nomeAtividade\":\"Original\"}", "nomeAtividade"),
            new Contrato("cursos", new CursoController(new CursoService()),
                "{\"educadoraId\":1,\"titulo\":\"Original\",\"descricao\":\"Descricao\",\"categoria\":\"Java\",\"cargaHoraria\":8}", "titulo"),
            new Contrato("empresas", new EmpresaController(new EmpresaService()),
                "{\"nome\":\"Original\",\"cnpj\":\"12345678000199\",\"email\":\"empresa@example.com\",\"senhaHash\":\"fixture-sem-credencial-real\"}", "nome"),
            new Contrato("mentorias", new MentoriaController(new MentoriaService()),
                "{\"educadoraId\":1,\"usuariaId\":1,\"titulo\":\"Original\",\"descricao\":\"Descricao\",\"dataHorario\":\"2030-01-01T10:00:00\"}", "titulo")
        );
    }

    @ParameterizedTest(name = "CRUD {0}")
    @MethodSource("contratos")
    void preservaCicloHttpEEstadoEmMemoria(Contrato contrato) throws Exception {
        MockMvc mvc = standaloneSetup(contrato.controller()).build();
        String rota = "/api/" + contrato.rota();
        mvc.perform(get(rota)).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get(rota + "/999")).andExpect(status().isNotFound());
        mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(contrato.dados()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(get(rota)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].senhaHash").doesNotExist());
        mvc.perform(put(rota + "/1").contentType(MediaType.APPLICATION_JSON)
                .content(contrato.dados().replace("Original", "Atualizado")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$." + contrato.campo()).value("Atualizado"));
        mvc.perform(get(rota + "/1")).andExpect(status().isOk())
            .andExpect(jsonPath("$." + contrato.campo()).value("Atualizado"));
        mvc.perform(delete(rota + "/1")).andExpect(status().isNoContent());
        mvc.perform(get(rota + "/1")).andExpect(status().isNotFound());
        mvc.perform(delete(rota + "/1")).andExpect(status().isNotFound());
        mvc.perform(get(rota)).andExpect(content().json("[]"));
    }

    @Test
    void preservaValidadoresAuxiliaresENotificacoes() throws Exception {
        for (Contrato contrato : contratos().toList()) {
            MockMvc mvc = standaloneSetup(contrato.controller()).build();
            String rota = "/api/" + contrato.rota();
            if (contrato.rota().equals("agenda")) {
                mvc.perform(post(rota + "/verificar-termino").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"dataFim\":\"2000-01-01T10:00:00\"}"))
                    .andExpect(status().isOk()).andExpect(content().string("O evento já terminou."));
            } else {
                String caminho = contrato.rota().equals("avaliacoes") ? "/validar-nota" : "/validar";
                String dados = contrato.rota().equals("certificados")
                    ? "{\"codigoValidacao\":\"00000000-0000-4000-8000-000000000001\"}" : contrato.dados();
                mvc.perform(post(rota + caminho).contentType(MediaType.APPLICATION_JSON).content(dados))
                    .andExpect(status().isOk());
                mvc.perform(post(rota + caminho).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
            }
        }
        MockMvc mvc = standaloneSetup(new NotificacaoController(new NotificacaoService())).build();
        mvc.perform(post("/api/notificacoes/verificar-leitura").contentType(MediaType.APPLICATION_JSON)
            .content("{\"lida\":true}"))
            .andExpect(status().isOk()).andExpect(content().string("Notificação marcada como lida."));
        mvc.perform(post("/api/notificacoes/verificar-leitura").contentType(MediaType.APPLICATION_JSON)
            .content("{\"lida\":false}"))
            .andExpect(status().isOk()).andExpect(content().string("Notificação não lida."));
    }
}
