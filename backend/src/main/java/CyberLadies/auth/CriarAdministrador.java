package CyberLadies.auth;

import java.sql.DriverManager;
import java.util.Arrays;

/** Comando explícito de terminal. Não inicia Spring, Flyway ou servidor HTTP. */
public final class CriarAdministrador {
    private CriarAdministrador() {}

    public static void main(String[] args) {
        char[] senha = null;
        char[] confirmacao = null;
        int codigo = 0;
        try {
            if (args.length != 1 || !args[0].startsWith("--banco=") || args[0].substring(8).isBlank())
                throw new IllegalArgumentException("Use somente --banco=NOME_EXATO_DO_BANCO. Não passe credenciais por argumentos.");
            var console = System.console();
            if (console == null)
                throw new IllegalArgumentException("Execute em um terminal interativo com entrada protegida. Pipes e entrada redirecionada não são aceitos.");
            String banco = args[0].substring(8);
            String url = obrigatoria("DATABASE_URL");
            String usuario = obrigatoria("DATABASE_USER");
            String senhaBanco = obrigatoria("DATABASE_PASSWORD");
            try (var conexao = DriverManager.getConnection(url,usuario,senhaBanco)) {
                AdministradorOperacao.conferirDestino(conexao,banco);
                try (var s = conexao.createStatement();
                     var r = s.executeQuery("SELECT current_database(),inet_server_addr()::text,inet_server_port()")) {
                    r.next();
                    console.printf("Destino conectado: banco=%s, servidor=%s, porta=%s%n",r.getString(1),r.getString(2),r.getString(3));
                }
                String nome = console.readLine("Nome da conta administrativa: ");
                String email = console.readLine("E-mail da nova conta dedicada: ");
                senha = console.readPassword("Senha (não será exibida): ");
                confirmacao = console.readPassword("Confirme a senha: ");
                if (senha == null || confirmacao == null || !Arrays.equals(senha,confirmacao))
                    throw new IllegalArgumentException("As senhas não coincidem ou a entrada foi cancelada.");
                long id = AdministradorOperacao.criar(conexao,new SenhasConfig().passwordEncoder(),banco,nome,email,senha);
                console.printf("Conta dedicada criada com somente ADMIN. ID: %d%n",id);
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            codigo = 1;
        } catch (Exception e) {
            // Não imprime exceções JDBC, parâmetros, senha ou hash.
            System.err.println("Não foi possível concluir a criação. Confira o destino e investigue antes de repetir.");
            codigo = 1;
        } finally {
            if (senha != null) Arrays.fill(senha,'\0');
            if (confirmacao != null) Arrays.fill(confirmacao,'\0');
        }
        if (codigo != 0) System.exit(codigo);
    }

    private static String obrigatoria(String nome) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Defina explicitamente " + nome + ".");
        return valor;
    }
}
