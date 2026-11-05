package CyberLadies.talk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
class ContaApi {
    private final JdbcClient db;
    private final Pbkdf2PasswordEncoder senhas = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final SecureRandom random = new SecureRandom();

    ContaApi(JdbcClient db) { this.db = db; }

    record Cadastro(@NotBlank @Size(max=120) String nome, @Email @NotBlank @Size(max=254) String email,
                    @NotBlank @Size(min=8,max=128) String senha, @Size(max=500) String competencias) {}
    record Login(@Email @NotBlank @Size(max=254) String email, @NotBlank @Size(max=128) String senha) {}
    record Credencial(long id, String hash) {}

    @PostMapping("/auth/cadastro/usuaria")
    @ResponseStatus(HttpStatus.CREATED)
    void cadastrar(@RequestBody @Valid Cadastro cadastro) {
        try {
            db.sql("INSERT INTO talk_usuaria(nome,email,senha_hash,competencias) VALUES (?,?,?,?)")
                .params(cadastro.nome().strip(), normalizar(cadastro.email()), senhas.encode(cadastro.senha()),
                        cadastro.competencias() == null ? "" : cadastro.competencias().strip()).update();
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
    }

    @PostMapping("/auth/login")
    @Transactional
    Map<String,String> entrar(@RequestBody @Valid Login login) {
        var conta = db.sql("SELECT id,senha_hash FROM talk_usuaria WHERE email=?")
            .param(normalizar(login.email()))
            .query((rs,n) -> new Credencial(rs.getLong("id"), rs.getString("senha_hash"))).optional();
        if (conta.isEmpty() || !senhas.matches(login.senha(), conta.get().hash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        db.sql("DELETE FROM talk_sessao WHERE expira_em <= CURRENT_TIMESTAMP").update();
        db.sql("INSERT INTO talk_sessao VALUES (?,?,CURRENT_TIMESTAMP + INTERVAL '8 hours')")
            .params(hash(token), conta.get().id()).update();
        return Map.of("token", token);
    }

    @DeleteMapping("/auth/sessao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sair(@RequestHeader(value="Authorization", defaultValue="") String auth) {
        db.sql("DELETE FROM talk_sessao WHERE token_hash=?").param(hashBearer(auth)).update();
    }

    static String hashBearer(String header) {
        if (header == null || !header.matches("Bearer [0-9a-f]{64}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre para acessar o CyberTalk.");
        }
        return hash(header.substring(7));
    }

    private static String normalizar(String email) { return email.strip().toLowerCase(Locale.ROOT); }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
