package CyberLadies.auth;

import java.nio.CharBuffer;
import java.sql.Connection;
import java.util.Locale;

import jakarta.validation.Validation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;

// Não é bean, serviço HTTP ou inicializador. Usado apenas pelo comando operacional.
final class AdministradorOperacao {
    private record Dados(@NotBlank @Size(max=120) String nome,
                         @NotBlank @Email @Size(max=254) String email) {}

    static void conferirDestino(Connection conexao, String banco) throws Exception {
        if (banco == null || banco.isBlank()) throw new IllegalArgumentException("Informe o nome exato do banco de destino.");
        try (var s = conexao.createStatement(); var r = s.executeQuery("SELECT current_database()")) {
            r.next();
            if (!banco.equals(r.getString(1))) throw new IllegalArgumentException("O banco conectado não corresponde ao destino informado.");
        }
    }

    static long criar(Connection conexao, PasswordEncoder encoder, String banco,
                      String nome, String email, char[] senha) throws Exception {
        conferirDestino(conexao,banco);
        var dados = new Dados(nome == null ? null : nome.strip(), email == null ? null : email.strip().toLowerCase(Locale.ROOT));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            if (!factory.getValidator().validate(dados).isEmpty())
                throw new IllegalArgumentException("Informe nome válido (até 120 caracteres) e e-mail válido (até 254).");
        }
        if (senha == null || senha.length < 8 || senha.length > 128 || CharBuffer.wrap(senha).chars().allMatch(Character::isWhitespace))
            throw new IllegalArgumentException("A senha deve ter de 8 a 128 caracteres e não pode ser vazia.");
        if (!conexao.getAutoCommit()) throw new IllegalArgumentException("Use uma conexão dedicada, sem transação em andamento.");
        String hash = encoder.encode(CharBuffer.wrap(senha));
        conexao.setAutoCommit(false);
        try {
            var db = JdbcClient.create(new SingleConnectionDataSource(conexao,true));
            long id = db.sql("INSERT INTO conta(nome,email,senha_hash) VALUES (?,?,?) RETURNING id")
                .params(dados.nome(),dados.email(),hash).query(Long.class).single();
            db.sql("INSERT INTO conta_papel(conta_id,papel) VALUES (?,'ADMIN')").param(id).update();
            conexao.commit();
            return id;
        } catch (DuplicateKeyException e) {
            conexao.rollback();
            throw new IllegalArgumentException("E-mail já cadastrado. Nenhuma conta existente foi modificada.");
        } catch (Exception e) {
            conexao.rollback();
            throw e;
        }
    }
}
