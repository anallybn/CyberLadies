CREATE TABLE perfil_certificacao_pdf (
    certificacao_id BIGINT PRIMARY KEY REFERENCES perfil_certificacao(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL CHECK (length(btrim(nome)) > 0),
    dados BYTEA NOT NULL CHECK (octet_length(dados) BETWEEN 1 AND 5242880)
);
