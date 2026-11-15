CREATE TABLE educadora (
    conta_id BIGINT PRIMARY KEY REFERENCES conta(id) ON DELETE CASCADE,
    especialidade VARCHAR(120) NOT NULL,
    instituicao VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    CONSTRAINT educadora_status_valido CHECK (status IN ('PENDENTE','APROVADA','REJEITADA'))
);
