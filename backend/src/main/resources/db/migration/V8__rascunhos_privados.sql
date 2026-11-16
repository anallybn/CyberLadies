-- Execução exclusiva no isolado. Não modifica tabelas de conteúdo/progresso.
CREATE TABLE IF NOT EXISTS academy_material_rascunho (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 educadora_id BIGINT NOT NULL REFERENCES educadora(conta_id),
 trilha_id BIGINT REFERENCES academy_autoria(trilha_id),
 titulo VARCHAR(160) NOT NULL CHECK (length(btrim(titulo))>0),
 descricao TEXT NOT NULL DEFAULT '' CHECK (length(descricao)<=5000),
 duracao_minutos INTEGER CHECK (duracao_minutos>0),
 tipo VARCHAR(12) NOT NULL DEFAULT 'VIDEO' CHECK (tipo='VIDEO'),
 tags VARCHAR(30)[] NOT NULL DEFAULT '{}' CHECK (cardinality(tags)<=8 AND array_position(tags,NULL) IS NULL),
 video_provedor VARCHAR(10) CHECK (video_provedor IN ('YOUTUBE','VIMEO')),
 video_id VARCHAR(20), uso_autorizado BOOLEAN NOT NULL DEFAULT FALSE,
 revisao BIGINT NOT NULL DEFAULT 1 CHECK (revisao>0),
 chave_criacao UUID NOT NULL, hash_criacao CHAR(64) NOT NULL,
 ultima_operacao UUID NOT NULL, hash_ultima_operacao CHAR(64) NOT NULL,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 atualizado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(educadora_id,chave_criacao),
 CHECK (video_id IS NULL OR (video_provedor IS NOT NULL AND (
  (video_provedor='YOUTUBE' AND video_id ~ '^[A-Za-z0-9_-]{11}$') OR
  (video_provedor='VIMEO' AND video_id ~ '^[1-9][0-9]{0,11}$'))))
);
CREATE INDEX IF NOT EXISTS academy_rascunho_educadora_atualizacao_idx
 ON academy_material_rascunho(educadora_id,atualizado_em DESC,id);
CREATE OR REPLACE FUNCTION academy_validar_rascunho() RETURNS TRIGGER LANGUAGE plpgsql AS '
BEGIN
 IF TG_OP=''UPDATE'' THEN
  IF NEW.educadora_id IS DISTINCT FROM OLD.educadora_id OR NEW.criado_em IS DISTINCT FROM OLD.criado_em
   OR NEW.chave_criacao IS DISTINCT FROM OLD.chave_criacao OR NEW.hash_criacao IS DISTINCT FROM OLD.hash_criacao THEN
   RAISE EXCEPTION ''Proprietária e identificação de criação são imutáveis.'' USING ERRCODE=''23514'';
  END IF;
  IF NEW.revisao<>OLD.revisao+1 THEN
   RAISE EXCEPTION ''A revisão deve avançar uma unidade.'' USING ERRCODE=''23514'';
  END IF;
 ELSE
  IF NEW.revisao<>1 THEN RAISE EXCEPTION ''Revisão inicial inválida.'' USING ERRCODE=''23514''; END IF;
  NEW.criado_em=clock_timestamp();
 END IF;
 IF NEW.trilha_id IS NOT NULL THEN
  PERFORM 1 FROM academy_autoria WHERE trilha_id=NEW.trilha_id AND educadora_id=NEW.educadora_id FOR KEY SHARE;
  IF NOT FOUND THEN RAISE EXCEPTION ''A trilha deve pertencer à Educadora.'' USING ERRCODE=''23514''; END IF;
 END IF;
 NEW.atualizado_em=clock_timestamp();
 RETURN NEW;
END';
CREATE OR REPLACE TRIGGER academy_rascunho_validar BEFORE INSERT OR UPDATE ON academy_material_rascunho
 FOR EACH ROW EXECUTE FUNCTION academy_validar_rascunho();
