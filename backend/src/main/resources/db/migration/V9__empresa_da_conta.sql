CREATE TABLE conta_empresa (
    conta_id BIGINT PRIMARY KEY REFERENCES conta(id) ON DELETE CASCADE,
    empresa_id BIGINT NOT NULL UNIQUE REFERENCES cyber_empresa(id),
    cnpj VARCHAR(14) NOT NULL UNIQUE CHECK (cnpj ~ '^[0-9]{14}$'),
    area_atuacao VARCHAR(160) NOT NULL CHECK (length(btrim(area_atuacao))>0)
);
