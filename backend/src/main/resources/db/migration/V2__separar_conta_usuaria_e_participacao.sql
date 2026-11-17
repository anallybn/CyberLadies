-- Executada em uma única transação pelo Flyway no PostgreSQL.
-- Renomear preserva IDs, identidades, sequências e referências existentes.
ALTER TABLE talk_usuaria RENAME TO conta;
ALTER TABLE talk_sessao RENAME TO auth_sessao;

CREATE TABLE usuaria (
    conta_id BIGINT PRIMARY KEY REFERENCES conta(id) ON DELETE CASCADE,
    competencias VARCHAR(500) NOT NULL
);

CREATE TABLE talk_participacao (
    conta_id BIGINT PRIMARY KEY REFERENCES conta(id) ON DELETE CASCADE
);

INSERT INTO usuaria(conta_id,competencias)
SELECT id,competencias FROM conta;

INSERT INTO talk_participacao(conta_id)
SELECT id FROM conta WHERE membro=true;

-- Só remove as colunas depois da transferência integral dos dados.
ALTER TABLE conta DROP COLUMN competencias, DROP COLUMN membro;
