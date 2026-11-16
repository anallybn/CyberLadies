package CyberLadies.auth.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SchemaAuth implements ApplicationRunner {
    private final JdbcClient db;
    public SchemaAuth(JdbcClient db) { this.db=db; }
    @Override public void run(ApplicationArguments args) {
        boolean pronto=db.sql("""
            SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema()
            AND table_name='talk_usuaria' AND column_name='tipo')
            AND to_regclass('auth_educadora') IS NOT NULL AND to_regclass('auth_empresa') IS NOT NULL
            """).query(Boolean.class).single();
        if(!pronto) throw new IllegalStateException("Migração auth-perfis-v1.sql pendente. Não é aplicada automaticamente. Consulte docs/AutenticacaoPerfis.md.");
    }
}