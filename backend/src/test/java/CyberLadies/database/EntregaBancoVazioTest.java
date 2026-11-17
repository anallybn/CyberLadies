package CyberLadies.database;

import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Cria e remove exclusivamente seu próprio banco efêmero. Não usa dados pessoais. */
@EnabledIfEnvironmentVariable(named="DATABASE_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/cyberladies_auth_test")
class EntregaBancoVazioTest {
    @Test void migraBancoRealVazioConfereRestricoesEReinicia() throws Exception {
        String nome="cyberladies_entrega_test_"+UUID.randomUUID().toString().replace("-","");
        String base="jdbc:postgresql://127.0.0.1:55439/";
        String usuario=System.getenv("DATABASE_USER"), senha=System.getenv("DATABASE_PASSWORD");
        try(var admin=DriverManager.getConnection(base+"postgres",usuario,senha);var stmt=admin.createStatement()) {
            stmt.execute("CREATE DATABASE "+nome);
            try {
                var flyway=Flyway.configure().dataSource(base+nome,usuario,senha).locations("classpath:db/migration")
                    .baselineOnMigrate(false).cleanDisabled(true).load();
                assertEquals(10,flyway.migrate().migrationsExecuted);
                flyway.validate();
                try(var banco=DriverManager.getConnection(base+nome,usuario,senha);var sql=banco.createStatement()) {
                    try(var rs=sql.executeQuery("SELECT count(*) FROM conta")) { rs.next(); assertEquals(0,rs.getInt(1)); }
                    try(var rs=sql.executeQuery("SELECT count(*) FROM pg_tables WHERE schemaname='public'")) {
                        rs.next(); assertTrue(rs.getInt(1)>20); System.out.println("Entrega banco vazio: tabelas="+rs.getInt(1));
                    }
                    try(var rs=sql.executeQuery("SELECT count(*) FROM pg_constraint WHERE contype='f' AND connamespace='public'::regnamespace")) {
                        rs.next(); assertTrue(rs.getInt(1)>20); System.out.println("Entrega banco vazio: chaves estrangeiras="+rs.getInt(1));
                    }
                    var fk=assertThrows(SQLException.class,() -> sql.execute("INSERT INTO talk_post(autora_id,texto) VALUES (999,'Sem autora')"));
                    assertEquals("23503",fk.getSQLState());
                    sql.execute("INSERT INTO conta(nome,email,senha_hash) VALUES ('Fixture','entrega@example.invalid','fixture')");
                    sql.execute("INSERT INTO usuaria(conta_id,competencias) SELECT id,'' FROM conta");
                    sql.execute("INSERT INTO talk_post(autora_id,texto) SELECT id,'Persistido' FROM conta");
                    var tamanho=assertThrows(SQLException.class,() -> sql.execute("INSERT INTO talk_post(autora_id,texto) SELECT id,repeat('a',501) FROM conta"));
                    assertEquals("22001",tamanho.getSQLState()); // VARCHAR(500): string_data_right_truncation.
                    var duplicado=assertThrows(SQLException.class,() -> sql.execute("INSERT INTO conta(nome,email,senha_hash) VALUES ('Outra','entrega@example.invalid','fixture')"));
                    assertEquals("23505",duplicado.getSQLState());
                }
                assertEquals(0,flyway.migrate().migrationsExecuted);
                try(var banco=DriverManager.getConnection(base+nome,usuario,senha);var sql=banco.createStatement();var rs=sql.executeQuery("SELECT texto FROM talk_post")) {
                    assertTrue(rs.next()); assertEquals("Persistido",rs.getString(1));
                }
            } finally { stmt.execute("DROP DATABASE "+nome); }
        }
    }
}
