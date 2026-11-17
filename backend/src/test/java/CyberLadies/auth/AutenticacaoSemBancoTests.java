package CyberLadies.auth;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Repositórios simulados: nenhum SQL, datasource ou servidor é inicializado. */
class AutenticacaoSemBancoTests {
    @Test void recusaBearerAusenteMalformadoOuOutroEsquema() {
        for (String header : new String[]{null, "", "Basic exemplo", "Bearer curto", "Bearer " + "G".repeat(64)}) {
            var erro = assertThrows(ResponseStatusException.class, () -> SessaoService.hashBearer(header));
            assertEquals(401, erro.getStatusCode().value());
        }
    }

    @Test void falhaDeLoginNaoRevelaExistenciaDaContaNemCriaSessao() {
        var contas = mock(ContaRepository.class);
        var sessoes = mock(SessaoRepository.class);
        var senhas = mock(PasswordEncoder.class);
        var service = new SessaoService(contas, sessoes, senhas);
        var login = new LoginRequest("conta@example.invalid", "senha-apenas-do-teste");
        when(contas.buscarCredencial(login.email())).thenReturn(Optional.empty());
        var ausente = assertThrows(ResponseStatusException.class, () -> service.entrar(login));
        when(contas.buscarCredencial(login.email())).thenReturn(Optional.of(new ContaRepository.Credencial(9, "hash-do-teste")));
        var incorreta = assertThrows(ResponseStatusException.class, () -> service.entrar(login));
        assertEquals(401, ausente.getStatusCode().value());
        assertEquals(ausente.getReason(), incorreta.getReason());
        verifyNoInteractions(sessoes);
    }

    @Test void loginEmiteTokenOpacoEApenasSeuHashSegueAoRepositorio() {
        var contas = mock(ContaRepository.class);
        var sessoes = mock(SessaoRepository.class);
        var senhas = mock(PasswordEncoder.class);
        when(contas.buscarCredencial("conta@example.invalid"))
            .thenReturn(Optional.of(new ContaRepository.Credencial(9, "hash-do-teste")));
        when(senhas.matches("senha-apenas-do-teste", "hash-do-teste")).thenReturn(true);
        var token = new SessaoService(contas, sessoes, senhas)
            .entrar(new LoginRequest(" CONTA@example.invalid ", "senha-apenas-do-teste"));
        assertTrue(token.matches("[0-9a-f]{64}"));
        var hash = SessaoService.hashBearer("Bearer " + token);
        assertNotEquals(token, hash);
        verify(sessoes).inserir(hash, 9L);
    }

    @Test void identidadeUsaSessaoEPapeisDoBackendIgnorandoPerfilDoCliente() {
        var sessoes = mock(SessaoRepository.class);
        var papeis = mock(PapelRepository.class);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + "a".repeat(64));
        request.addParameter("perfil", "ADMIN");
        when(sessoes.buscarConta(anyString())).thenReturn(Optional.of(9L));
        when(papeis.buscar(9L)).thenReturn(Set.of(Papel.USUARIA));
        var acesso = new AcessoAutenticado(sessoes, request, papeis);
        assertEquals(Set.of(Papel.USUARIA), acesso.identidade().papeis());
        when(sessoes.buscarConta(anyString())).thenReturn(Optional.empty());
        assertEquals(401, assertThrows(ResponseStatusException.class, acesso::identidade).getStatusCode().value());
    }
}
