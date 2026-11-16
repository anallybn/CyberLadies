package CyberLadies.auth;

import java.nio.CharBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

/** Operação explícita de terminal. Não é bean, endpoint ou inicializador. */
public final class AlterarSenhaAdministrador {
    private AlterarSenhaAdministrador() {}

    static long alterar(Connection conexao, PasswordEncoder encoder, String banco, String email, char[] senha) throws Exception {
        AdministradorOperacao.conferirDestino(conexao,banco);
        if (email==null || email.isBlank()) throw new IllegalArgumentException("Informe o e-mail do ADMIN existente.");
        if (senha==null || senha.length<8 || senha.length>128 || CharBuffer.wrap(senha).chars().allMatch(Character::isWhitespace))
            throw new IllegalArgumentException("A senha deve ter de 8 a 128 caracteres e não pode ser vazia.");
        if (!conexao.getAutoCommit()) throw new IllegalArgumentException("Use uma conexão dedicada.");
        conexao.setAutoCommit(false);
        try {
            long id;
            try (var s=conexao.prepareStatement("SELECT c.id FROM conta c WHERE c.email=? AND EXISTS(SELECT 1 FROM conta_papel p WHERE p.conta_id=c.id AND p.papel='ADMIN') FOR UPDATE")) {
                s.setString(1,email.strip().toLowerCase(Locale.ROOT));
                try (var r=s.executeQuery()) {
                    if (!r.next()) throw new IllegalArgumentException("ADMIN existente não encontrado. Nenhuma conta foi criada ou alterada.");
                    id=r.getLong(1);
                }
            }
            try (var s=conexao.prepareStatement("UPDATE conta SET senha_hash=? WHERE id=?")) {
                s.setString(1,encoder.encode(CharBuffer.wrap(senha))); s.setLong(2,id);
                if (s.executeUpdate()!=1) throw new IllegalArgumentException("Conta não foi atualizada.");
            }
            conexao.commit(); return id;
        } catch (Exception e) { conexao.rollback(); throw e; }
    }

    public static void main(String[] args) {
        char[] senha=null, confirmacao=null;
        boolean atualizada=false;
        int codigo=0;
        try {
            if(args.length!=1 || !args[0].startsWith("--email=") || args[0].substring(8).isBlank())
                throw new IllegalArgumentException("Use somente --email=EMAIL_DO_ADMIN. Nunca passe senha por argumentos.");
            var console=System.console();
            if(console==null) throw new IllegalArgumentException("Execute em terminal interativo. Entrada redirecionada ou sem proteção não é aceita.");
            String email=args[0].substring(8).strip().toLowerCase(Locale.ROOT);
            String url=System.getenv("DATABASE_URL");
            if(!"jdbc:postgresql://127.0.0.1:55439/cyberladies_apresentacao".equals(url))
                throw new IllegalArgumentException("Destino recusado: este comando exige cyberladies_apresentacao em 127.0.0.1:55439.");
            String usuario=System.getenv("DATABASE_USER");
            if(usuario==null || usuario.isBlank()) throw new IllegalArgumentException("DATABASE_USER não configurado.");
            try(var conexao=DriverManager.getConnection(url,usuario,System.getenv().getOrDefault("DATABASE_PASSWORD",""))) {
                AdministradorOperacao.conferirDestino(conexao,"cyberladies_apresentacao");
                console.printf("Banco: cyberladies_apresentacao, porta 55439. ADMIN: %s%n",email);
                senha=console.readPassword("Nova senha (entrada protegida): ");
                confirmacao=console.readPassword("Confirme a nova senha: ");
                if(senha==null || confirmacao==null || !Arrays.equals(senha,confirmacao))
                    throw new IllegalArgumentException("As senhas não coincidem ou a operação foi cancelada. Nada foi alterado.");
                long id=alterar(conexao,new SenhasConfig().passwordEncoder(),"cyberladies_apresentacao",email,senha);
                atualizada=true;
                validarLogin(email,senha,id);
                console.printf("Senha atualizada e login validado por 5178. ID %d e papel ADMIN preservados. Senha e token não foram exibidos ou gravados.%n",id);
            }
        } catch(IllegalArgumentException e) {
            System.err.println(atualizada ? "Senha atualizada, mas a validação HTTP não foi concluída. Não redefina novamente sem conferir a API." : e.getMessage()); codigo=1;
        } catch(Exception e) {
            System.err.println(atualizada ? "Senha atualizada, mas não foi possível validar o login pela API. Confira o ambiente em 5178." : "Não foi possível atualizar a senha. Confira o destino e a conta ADMIN existente; nenhuma duplicata foi criada."); codigo=1;
        } finally {
            if(senha!=null) Arrays.fill(senha,'\0');
            if(confirmacao!=null) Arrays.fill(confirmacao,'\0');
        }
        if(codigo!=0) System.exit(codigo);
    }

    private static void validarLogin(String email,char[] senha,long id) throws Exception {
        var json=JsonMapper.builder().build();
        var cliente=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var resposta=cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:5178/api/auth/login"))
            .timeout(Duration.ofSeconds(15)).header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email",email,"senha",new String(senha))))).build(),HttpResponse.BodyHandlers.ofString());
        if(resposta.statusCode()!=200) throw new IllegalStateException("Login não validado.");
        String token=json.readTree(resposta.body()).get("token").asString();
        try {
            var me=cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:5178/api/auth/me"))
                .timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(me.statusCode()!=200) throw new IllegalStateException("Identidade não validada.");
            var identidade=json.readTree(me.body());
            if(identidade.get("id").asLong()!=id || !identidade.get("papeis").toString().contains("\"ADMIN\""))
                throw new IllegalStateException("Identidade inesperada.");
        } finally {
            cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:5178/api/auth/sessao"))
                .timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token).DELETE().build(),HttpResponse.BodyHandlers.discarding());
        }
    }
}
