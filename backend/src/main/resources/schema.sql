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
-- CyberAcademy: catálogo administrativo; não contém seed.
CREATE TABLE IF NOT EXISTS academy_trilha (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 titulo VARCHAR(160) NOT NULL CHECK (length(trim(titulo))>0),
 descricao TEXT NOT NULL,
 categoria VARCHAR(20) NOT NULL CHECK (categoria IN ('FRONTEND','BACKEND','DADOS','UX_DESIGN','SOFT_SKILLS')),
 nivel VARCHAR(20) NOT NULL CHECK (nivel IN ('INICIANTE','INTERMEDIARIO','AVANCADO')),
 versao INTEGER NOT NULL DEFAULT 1 CHECK (versao>0),
 origem_id BIGINT REFERENCES academy_trilha(id),
 elegivel_certificado BOOLEAN NOT NULL DEFAULT FALSE,
 congelada BOOLEAN NOT NULL DEFAULT FALSE,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS academy_aula (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 trilha_id BIGINT NOT NULL REFERENCES academy_trilha(id),
 titulo VARCHAR(160) NOT NULL CHECK (length(trim(titulo))>0),
 descricao TEXT NOT NULL,
 complementar TEXT NOT NULL DEFAULT '',
 ordem INTEGER NOT NULL CHECK (ordem>0),
 duracao_minutos INTEGER NOT NULL CHECK (duracao_minutos>0),
 video_provedor VARCHAR(10) NOT NULL CHECK (video_provedor IN ('YOUTUBE','VIMEO')),
 video_id VARCHAR(20) NOT NULL,
 uso_autorizado BOOLEAN NOT NULL CHECK (uso_autorizado),
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK ((video_provedor='YOUTUBE' AND video_id ~ '^[A-Za-z0-9_-]{11}$')
     OR (video_provedor='VIMEO' AND video_id ~ '^[1-9][0-9]{0,11}$')),
 UNIQUE (trilha_id,ordem), UNIQUE(id,trilha_id)
);
CREATE TABLE IF NOT EXISTS academy_inscricao (
 usuaria_id BIGINT NOT NULL REFERENCES talk_usuaria(id),
 trilha_id BIGINT NOT NULL REFERENCES academy_trilha(id),
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(usuaria_id,trilha_id)
);
CREATE TABLE IF NOT EXISTS academy_conclusao (
 usuaria_id BIGINT NOT NULL,
 trilha_id BIGINT NOT NULL,
 aula_id BIGINT NOT NULL,
 concluido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(usuaria_id,aula_id),
 FOREIGN KEY(usuaria_id,trilha_id) REFERENCES academy_inscricao(usuaria_id,trilha_id),
 FOREIGN KEY(aula_id,trilha_id) REFERENCES academy_aula(id,trilha_id)
);
CREATE TABLE IF NOT EXISTS academy_mentora (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 usuaria_id BIGINT UNIQUE REFERENCES talk_usuaria(id),
 nome VARCHAR(120) NOT NULL CHECK (length(trim(nome))>0),
 especialidade VARCHAR(160) NOT NULL,
 descricao VARCHAR(2000) NOT NULL DEFAULT '',
 anos_experiencia INTEGER CHECK (anos_experiencia>=0)
);
CREATE TABLE IF NOT EXISTS academy_disponibilidade (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 mentora_id BIGINT NOT NULL REFERENCES academy_mentora(id),
 inicio TIMESTAMPTZ NOT NULL, fim TIMESTAMPTZ NOT NULL,
 CHECK (fim>inicio), UNIQUE(mentora_id,inicio)
);
CREATE TABLE IF NOT EXISTS academy_agendamento (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 usuaria_id BIGINT NOT NULL REFERENCES talk_usuaria(id),
 disponibilidade_id BIGINT NOT NULL REFERENCES academy_disponibilidade(id),
 status VARCHAR(12) NOT NULL DEFAULT 'CONFIRMADO' CHECK (status IN ('CONFIRMADO','CANCELADO')),
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS academy_reserva_ativa_idx ON academy_agendamento(disponibilidade_id) WHERE status='CONFIRMADO';
CREATE INDEX IF NOT EXISTS academy_agendamento_usuaria_idx ON academy_agendamento(usuaria_id);
CREATE INDEX IF NOT EXISTS academy_disponibilidade_mentora_idx ON academy_disponibilidade(mentora_id);
CREATE INDEX IF NOT EXISTS academy_inscricao_trilha_idx ON academy_inscricao(trilha_id);
CREATE INDEX IF NOT EXISTS academy_conclusao_trilha_idx ON academy_conclusao(usuaria_id,trilha_id);
CREATE TABLE IF NOT EXISTS academy_certificado (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 usuaria_id BIGINT NOT NULL,
 trilha_id BIGINT NOT NULL,
 nome_usuaria VARCHAR(120) NOT NULL,
 titulo_trilha VARCHAR(160) NOT NULL,
 versao INTEGER NOT NULL,
 total_aulas BIGINT NOT NULL CHECK (total_aulas>0),
 carga_minutos BIGINT NOT NULL CHECK (carga_minutos>0),
 emitido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(usuaria_id,trilha_id) REFERENCES academy_inscricao(usuaria_id,trilha_id),
 UNIQUE(usuaria_id,trilha_id)
);

-- Todas as alterações de conteúdo e inscrições disputam a mesma linha da trilha.
CREATE OR REPLACE FUNCTION academy_proteger_trilha() RETURNS TRIGGER LANGUAGE plpgsql AS '
BEGIN
 IF OLD.congelada THEN
   RAISE EXCEPTION ''Trilha congelada: crie uma nova versão.'' USING ERRCODE=''23514'';
 END IF;
 IF TG_OP=''DELETE'' THEN RETURN OLD; END IF;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER academy_trilha_imutavel BEFORE UPDATE OR DELETE ON academy_trilha FOR EACH ROW EXECUTE FUNCTION academy_proteger_trilha();

CREATE OR REPLACE FUNCTION academy_proteger_aula() RETURNS TRIGGER LANGUAGE plpgsql AS '
DECLARE trilha BIGINT; bloqueada BOOLEAN;
BEGIN
 IF TG_OP=''UPDATE'' AND NEW.trilha_id<>OLD.trilha_id THEN
   RAISE EXCEPTION ''Uma aula não pode mudar de trilha.'' USING ERRCODE=''23514'';
 END IF;
 IF TG_OP=''DELETE'' THEN trilha=OLD.trilha_id; ELSE trilha=NEW.trilha_id; END IF;
 SELECT congelada INTO bloqueada FROM academy_trilha WHERE id=trilha FOR UPDATE;
 IF bloqueada THEN RAISE EXCEPTION ''Conteúdo e ordem congelados após a primeira inscrição.'' USING ERRCODE=''23514''; END IF;
 IF TG_OP=''DELETE'' THEN RETURN OLD; END IF;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER academy_aula_imutavel BEFORE INSERT OR UPDATE OR DELETE ON academy_aula FOR EACH ROW EXECUTE FUNCTION academy_proteger_aula();

CREATE OR REPLACE FUNCTION academy_inscrever() RETURNS TRIGGER LANGUAGE plpgsql AS '
BEGIN
 PERFORM id FROM academy_trilha WHERE id=NEW.trilha_id FOR UPDATE;
 IF NOT EXISTS(SELECT 1 FROM academy_aula WHERE trilha_id=NEW.trilha_id) THEN
   RAISE EXCEPTION ''Trilha sem aulas disponíveis.'' USING ERRCODE=''23514'';
 END IF;
 UPDATE academy_trilha SET congelada=true WHERE id=NEW.trilha_id AND congelada=false;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER academy_inscricao_congelar BEFORE INSERT ON academy_inscricao FOR EACH ROW EXECUTE FUNCTION academy_inscrever();

-- Serializa conclusões diferentes da mesma inscrição, inclusive sob concorrência.
CREATE OR REPLACE FUNCTION academy_bloquear_progresso() RETURNS TRIGGER LANGUAGE plpgsql AS '
BEGIN
 PERFORM 1 FROM academy_inscricao WHERE usuaria_id=NEW.usuaria_id AND trilha_id=NEW.trilha_id FOR UPDATE;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER academy_conclusao_bloqueio BEFORE INSERT ON academy_conclusao FOR EACH ROW EXECUTE FUNCTION academy_bloquear_progresso();

CREATE OR REPLACE FUNCTION academy_validar_certificado() RETURNS TRIGGER LANGUAGE plpgsql AS '
DECLARE total BIGINT; feitas BIGINT;
BEGIN
 IF TG_OP=''UPDATE'' THEN RAISE EXCEPTION ''Certificado emitido é imutável.'' USING ERRCODE=''23514''; END IF;
 PERFORM 1 FROM academy_inscricao WHERE usuaria_id=NEW.usuaria_id AND trilha_id=NEW.trilha_id FOR UPDATE;
 SELECT count(*),COALESCE(sum(duracao_minutos),0) INTO total,NEW.carga_minutos FROM academy_aula WHERE trilha_id=NEW.trilha_id;
 SELECT count(*) INTO feitas FROM academy_conclusao WHERE usuaria_id=NEW.usuaria_id AND trilha_id=NEW.trilha_id;
 IF total=0 OR feitas<>total OR NOT EXISTS(SELECT 1 FROM academy_trilha WHERE id=NEW.trilha_id AND elegivel_certificado) THEN
   RAISE EXCEPTION ''Trilha não concluída ou não elegível.'' USING ERRCODE=''23514'';
 END IF;
 NEW.total_aulas=total;
 SELECT nome INTO NEW.nome_usuaria FROM talk_usuaria WHERE id=NEW.usuaria_id;
 SELECT titulo,versao INTO NEW.titulo_trilha,NEW.versao FROM academy_trilha WHERE id=NEW.trilha_id;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER academy_certificado_validar BEFORE INSERT OR UPDATE ON academy_certificado FOR EACH ROW EXECUTE FUNCTION academy_validar_certificado();

CREATE OR REPLACE FUNCTION academy_emitir_certificado() RETURNS TRIGGER LANGUAGE plpgsql AS '
BEGIN
 IF EXISTS(SELECT 1 FROM academy_trilha WHERE id=NEW.trilha_id AND elegivel_certificado)
   AND (SELECT count(*) FROM academy_aula WHERE trilha_id=NEW.trilha_id)>0
   AND (SELECT count(*) FROM academy_aula WHERE trilha_id=NEW.trilha_id)=
       (SELECT count(*) FROM academy_conclusao WHERE usuaria_id=NEW.usuaria_id AND trilha_id=NEW.trilha_id) THEN
   INSERT INTO academy_certificado(usuaria_id,trilha_id,nome_usuaria,titulo_trilha,versao,total_aulas,carga_minutos)
   VALUES (NEW.usuaria_id,NEW.trilha_id,'''','''',1,1,1) ON CONFLICT(usuaria_id,trilha_id) DO NOTHING;
 END IF;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER academy_conclusao_certificado AFTER INSERT ON academy_conclusao FOR EACH ROW EXECUTE FUNCTION academy_emitir_certificado();

CREATE OR REPLACE FUNCTION academy_validar_disponibilidade() RETURNS TRIGGER LANGUAGE plpgsql AS '
BEGIN
 IF TG_OP IN (''UPDATE'',''DELETE'') AND EXISTS(SELECT 1 FROM academy_agendamento WHERE disponibilidade_id=OLD.id) THEN
   RAISE EXCEPTION ''Horário com histórico de agendamento é imutável.'' USING ERRCODE=''23514'';
 END IF;
 IF TG_OP=''DELETE'' THEN RETURN OLD; END IF;
 IF TG_OP=''UPDATE'' AND NEW.mentora_id<>OLD.mentora_id THEN
   RAISE EXCEPTION ''Não é permitido transferir disponibilidade.'' USING ERRCODE=''23514'';
 END IF;
 PERFORM id FROM academy_mentora WHERE id=NEW.mentora_id FOR UPDATE;
 IF NEW.inicio<=clock_timestamp() THEN RAISE EXCEPTION ''Horário passado.'' USING ERRCODE=''23514''; END IF;
 IF EXISTS(SELECT 1 FROM academy_disponibilidade WHERE mentora_id=NEW.mentora_id AND id<>NEW.id
    AND inicio<NEW.fim AND fim>NEW.inicio) THEN
   RAISE EXCEPTION ''Disponibilidades sobrepostas.'' USING ERRCODE=''23514'';
 END IF;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER academy_disponibilidade_validar BEFORE INSERT OR UPDATE OR DELETE ON academy_disponibilidade FOR EACH ROW EXECUTE FUNCTION academy_validar_disponibilidade();

-- Ordem de bloqueio única: usuária, mentora, disponibilidade.
CREATE OR REPLACE FUNCTION academy_validar_agendamento() RETURNS TRIGGER LANGUAGE plpgsql AS '
DECLARE horario academy_disponibilidade%ROWTYPE;
BEGIN
 IF TG_OP=''UPDATE'' AND (NEW.usuaria_id<>OLD.usuaria_id OR NEW.disponibilidade_id<>OLD.disponibilidade_id
    OR OLD.status<>''CONFIRMADO'' OR NEW.status<>''CANCELADO'') THEN
   RAISE EXCEPTION ''Somente cancelamento do agendamento original é permitido.'' USING ERRCODE=''23514'';
 END IF;
 SELECT * INTO horario FROM academy_disponibilidade WHERE id=NEW.disponibilidade_id;
 IF NOT FOUND THEN RAISE EXCEPTION ''Horário inexistente.'' USING ERRCODE=''23503''; END IF;
 PERFORM id FROM talk_usuaria WHERE id=NEW.usuaria_id FOR UPDATE;
 PERFORM id FROM academy_mentora WHERE id=horario.mentora_id FOR UPDATE;
 SELECT * INTO horario FROM academy_disponibilidade WHERE id=NEW.disponibilidade_id FOR UPDATE;
 IF horario.inicio<=clock_timestamp() THEN RAISE EXCEPTION ''Horário já iniciado.'' USING ERRCODE=''23514''; END IF;
 IF TG_OP=''INSERT'' THEN
   IF NEW.status<>''CONFIRMADO'' THEN RAISE EXCEPTION ''Agendamento deve iniciar confirmado.'' USING ERRCODE=''23514''; END IF;
   IF EXISTS(SELECT 1 FROM academy_agendamento a JOIN academy_disponibilidade d ON d.id=a.disponibilidade_id
      WHERE a.status=''CONFIRMADO'' AND d.inicio<horario.fim AND d.fim>horario.inicio
      AND (a.usuaria_id=NEW.usuaria_id OR d.mentora_id=horario.mentora_id)) THEN
      RAISE EXCEPTION ''Horário ocupado ou sobreposto.'' USING ERRCODE=''23505'';
   END IF;
 END IF;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER academy_agendamento_validar BEFORE INSERT OR UPDATE ON academy_agendamento FOR EACH ROW EXECUTE FUNCTION academy_validar_agendamento();