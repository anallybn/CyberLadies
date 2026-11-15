package CyberLadies.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class SessaoService {
    private final ContaRepository contas;
    private final SessaoRepository sessoes;
    private final PasswordEncoder senhas;
    private final SecureRandom random = new SecureRandom();

    SessaoService(ContaRepository contas, SessaoRepository sessoes, PasswordEncoder senhas) {
        this.contas = contas;
        this.sessoes = sessoes;
        this.senhas = senhas;
    }

    @Transactional
    public String entrar(LoginRequest login) {
        var conta = contas.buscarCredencial(login.email().strip().toLowerCase(Locale.ROOT));
        if (conta.isEmpty() || !senhas.matches(login.senha(), conta.get().hash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        sessoes.removerExpiradas();
        sessoes.inserir(hash(token), conta.get().id());
        return token;
    }

    public void sair(String auth) {
        sessoes.remover(hashBearer(auth));
    }

    static String hashBearer(String header) {
        if (header == null || !header.matches("Bearer [0-9a-f]{64}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre para acessar o CyberTalk.");
        }
        return hash(header.substring(7));
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
