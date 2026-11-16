CREATE TABLE IF NOT EXISTS talk_usuaria (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    competencias VARCHAR(500) NOT NULL,
    membro BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS talk_sessao (
    token_hash VARCHAR(64) PRIMARY KEY,
    usuaria_id BIGINT NOT NULL REFERENCES talk_usuaria(id) ON DELETE CASCADE,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE IF NOT EXISTS talk_post (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    autora_id BIGINT NOT NULL REFERENCES talk_usuaria(id),
    texto VARCHAR(500) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS talk_post_autora_idx ON talk_post(autora_id);
CREATE TABLE IF NOT EXISTS talk_curtida (
    post_id BIGINT NOT NULL REFERENCES talk_post(id) ON DELETE CASCADE,
    usuaria_id BIGINT NOT NULL REFERENCES talk_usuaria(id) ON DELETE CASCADE,
    PRIMARY KEY(post_id, usuaria_id)
);
CREATE TABLE IF NOT EXISTS talk_comentario (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES talk_post(id) ON DELETE CASCADE,
    autora_id BIGINT NOT NULL REFERENCES talk_usuaria(id),
    texto VARCHAR(500) NOT NULL
);
CREATE INDEX IF NOT EXISTS talk_comentario_post_idx ON talk_comentario(post_id);

CREATE TABLE IF NOT EXISTS talk_anexo (
    post_id BIGINT PRIMARY KEY REFERENCES talk_post(id) ON DELETE CASCADE,
    nome VARCHAR(200) NOT NULL,
    tipo VARCHAR(80) NOT NULL,
    dados BYTEA NOT NULL
);

-- CyberVagas: migração aditiva, sem dados de demonstração automáticos.
CREATE TABLE IF NOT EXISTS cyber_empresa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(120) NOT NULL CHECK (length(trim(nome)) > 0)
);
CREATE TABLE IF NOT EXISTS cyber_vaga (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES cyber_empresa(id),
    titulo VARCHAR(160) NOT NULL CHECK (length(trim(titulo)) > 0),
    descricao TEXT NOT NULL,
    requisitos TEXT NOT NULL DEFAULT '',
    modalidade VARCHAR(12) CHECK (modalidade IN ('REMOTO','HIBRIDO','PRESENCIAL')),
    nivel VARCHAR(10) CHECK (nivel IN ('ESTAGIO','JUNIOR','PLENO','SENIOR')),
    localizacao VARCHAR(160),
    salario_minimo NUMERIC(12,2) CHECK (salario_minimo >= 0),
    salario_maximo NUMERIC(12,2) CHECK (salario_maximo >= 0),
    tecnologias VARCHAR(1000) NOT NULL DEFAULT '',
    para_maes BOOLEAN NOT NULL DEFAULT FALSE,
    pcd BOOLEAN NOT NULL DEFAULT FALSE,
    sem_formacao BOOLEAN NOT NULL DEFAULT FALSE,
    publicado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (salario_maximo IS NULL OR salario_minimo IS NULL OR salario_maximo >= salario_minimo)
);
CREATE INDEX IF NOT EXISTS cyber_vaga_empresa_idx ON cyber_vaga(empresa_id);
CREATE TABLE IF NOT EXISTS cyber_candidatura (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuaria_id BIGINT NOT NULL REFERENCES talk_usuaria(id),
    vaga_id BIGINT NOT NULL REFERENCES cyber_vaga(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' CHECK (status = 'PENDENTE'),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (usuaria_id,vaga_id)
);
CREATE INDEX IF NOT EXISTS cyber_candidatura_vaga_idx ON cyber_candidatura(vaga_id);
