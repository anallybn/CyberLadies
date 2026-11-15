package CyberLadies.database;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.springframework.core.io.ClassPathResource;

/** Ferramenta operacional explícita. Não é um bean e não roda ao iniciar a aplicação. */
public final class AdotarBanco {
    private AdotarBanco() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("--verificar") || args[0].startsWith("--adotar="))) {
            throw new IllegalArgumentException("Use --verificar ou --adotar=NOME_EXATO_DO_BANCO, com a aplicação parada.");
        }
        String url = obrigatoria("DATABASE_URL");
        String usuario = obrigatoria("DATABASE_USER");
        String senha = obrigatoria("DATABASE_PASSWORD");
        executar(url, usuario, senha, args[0]);
    }

    static void executar(String url, String usuario, String senha, String operacao) throws Exception {
        if (!(operacao.equals("--verificar") || operacao.startsWith("--adotar="))) {
            throw new IllegalArgumentException("Operação de adoção inválida.");
        }
        try (var conexao = DriverManager.getConnection(url, usuario, senha)) {
            conexao.setReadOnly(true);
            conexao.setAutoCommit(false);
            try (var statement = conexao.createStatement()) {
                statement.execute("SET LOCAL search_path TO public, pg_catalog");
                try (var resultado = statement.executeQuery("SELECT current_database()")) {
                    resultado.next();
                    String banco = resultado.getString(1);
                    if (!operacao.equals("--verificar") && !operacao.equals("--adotar=" + banco)) {
                        throw new IllegalArgumentException("O nome informado não corresponde ao banco conectado.");
                    }
                    System.out.println("Conferindo schema public de " + banco + ".");
                }
            }
            verificar(conexao);
            // Bloqueia DDL que exija lock exclusivo; a janela sem alterações continua obrigatória.
            try (var statement = conexao.createStatement()) {
                statement.execute("""
                    LOCK TABLE public.talk_usuaria, public.talk_sessao, public.talk_post,
                    public.talk_comentario, public.talk_curtida, public.talk_anexo IN ACCESS SHARE MODE
                    """);
            }
            verificar(conexao);
            System.out.println("Schema compatível com V1. Nenhum dado de negócio foi alterado.");
            if (operacao.startsWith("--adotar=")) {
                flyway(url, usuario, senha).baseline();
                System.out.println("Baseline V1 registrado. Inicie a aplicação normalmente.");
            }
            conexao.rollback();
        }
    }

    static Flyway flyway(String url, String usuario, String senha) {
        return Flyway.configure().dataSource(url, usuario, senha).schemas("public").defaultSchema("public")
            .locations("classpath:db/migration").baselineVersion("1")
            .baselineDescription("Schema anterior conferido como V1")
            .baselineOnMigrate(false).cleanDisabled(true).load();
    }

    static void verificar(Connection conexao) throws Exception {
        List<String> esperado = recurso("db/schema_v1.assinatura").lines().filter(linha -> !linha.isBlank()).toList();
        var atual = new ArrayList<String>();
        try (var statement = conexao.createStatement();
             var resultado = statement.executeQuery(recurso("db/verificar_schema_v1.sql"))) {
            while (resultado.next()) atual.add(resultado.getString(1));
        }
        if (!esperado.equals(atual)) {
            var faltando = esperado.stream().filter(item -> !atual.contains(item)).toList();
            var inesperado = atual.stream().filter(item -> !esperado.contains(item)).toList();
            throw new IllegalStateException("Adoção recusada: schema diferente de V1 ou já gerenciado. Faltando: "
                + faltando + "; inesperado: " + inesperado);
        }
    }

    private static String recurso(String caminho) throws Exception {
        return new ClassPathResource(caminho).getContentAsString(StandardCharsets.UTF_8);
    }

    private static String obrigatoria(String nome) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Defina explicitamente " + nome + ".");
        return valor;
    }
}
