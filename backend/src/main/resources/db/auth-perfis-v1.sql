-- Fase 1: execução explícita, fora do schema.sql automático.
-- No principal, aplicar somente após backup testado e confirmação da usuária.
ALTER TABLE talk_usuaria ADD COLUMN IF NOT EXISTS tipo VARCHAR(12) NOT NULL DEFAULT 'CYBERLADIE'
 CHECK (tipo IN ('CYBERLADIE','EDUCADORA','EMPRESA'));
CREATE UNIQUE INDEX IF NOT EXISTS auth_email_normalizado_idx ON talk_usuaria(lower(btrim(email)));
CREATE TABLE IF NOT EXISTS auth_educadora (
 usuaria_id BIGINT PRIMARY KEY REFERENCES talk_usuaria(id),
 especialidade VARCHAR(160) NOT NULL CHECK (length(trim(especialidade))>0),
 instituicao VARCHAR(160) NOT NULL CHECK (length(trim(instituicao))>0),
 status VARCHAR(12) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','APROVADA','REJEITADA'))
);
CREATE TABLE IF NOT EXISTS auth_empresa (
 usuaria_id BIGINT PRIMARY KEY REFERENCES talk_usuaria(id),
 empresa_id BIGINT NOT NULL UNIQUE REFERENCES cyber_empresa(id),
 cnpj VARCHAR(14) NOT NULL UNIQUE CHECK (cnpj ~ '^[0-9]{14}$'),
 area_atuacao VARCHAR(160) NOT NULL CHECK (length(trim(area_atuacao))>0)
);
CREATE OR REPLACE FUNCTION auth_preservar_tipo() RETURNS TRIGGER LANGUAGE plpgsql AS '
BEGIN
 IF NEW.tipo IS DISTINCT FROM OLD.tipo THEN
  RAISE EXCEPTION ''Tipo de conta é imutável nesta fase.'' USING ERRCODE=''23514'';
 END IF;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER auth_tipo_imutavel BEFORE UPDATE OF tipo ON talk_usuaria FOR EACH ROW EXECUTE FUNCTION auth_preservar_tipo();
CREATE OR REPLACE FUNCTION auth_validar_perfil() RETURNS TRIGGER LANGUAGE plpgsql AS '
DECLARE esperado TEXT; real_tipo TEXT;
BEGIN
 IF TG_TABLE_NAME=''auth_educadora'' THEN esperado=''EDUCADORA''; ELSE esperado=''EMPRESA''; END IF;
 SELECT tipo INTO real_tipo FROM talk_usuaria WHERE id=NEW.usuaria_id;
 IF real_tipo IS DISTINCT FROM esperado THEN
  RAISE EXCEPTION ''Perfil incompatível com a conta.'' USING ERRCODE=''23514'';
 END IF;
 IF TG_OP=''UPDATE'' AND NEW.usuaria_id<>OLD.usuaria_id THEN
  RAISE EXCEPTION ''Não é permitido transferir o perfil.'' USING ERRCODE=''23514'';
 END IF;
 IF TG_OP=''INSERT'' AND TG_TABLE_NAME=''auth_educadora'' AND to_jsonb(NEW)->>''status''<>''PENDENTE'' THEN
  RAISE EXCEPTION ''Educadora deve iniciar pendente.'' USING ERRCODE=''23514'';
 END IF;
 RETURN NEW;
END ';
CREATE OR REPLACE TRIGGER auth_educadora_validar BEFORE INSERT OR UPDATE ON auth_educadora FOR EACH ROW EXECUTE FUNCTION auth_validar_perfil();
CREATE OR REPLACE TRIGGER auth_empresa_validar BEFORE INSERT OR UPDATE ON auth_empresa FOR EACH ROW EXECUTE FUNCTION auth_validar_perfil();