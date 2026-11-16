-- Apenas ambiente isolado nesta etapa. Não atribui trilhas existentes.
DO 'BEGIN IF current_database() !~ ''^cyberladies_auth_[a-z0-9_]+$'' THEN
 RAISE EXCEPTION ''Dashboard: migração autorizada somente no banco isolado da Fase 1.'';
 END IF; END';
CREATE TABLE IF NOT EXISTS academy_autoria (
 trilha_id BIGINT PRIMARY KEY REFERENCES academy_trilha(id),
 educadora_id BIGINT NOT NULL REFERENCES auth_educadora(usuaria_id),
 vinculado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS academy_autoria_educadora_idx ON academy_autoria(educadora_id);
-- Todo conteúdo atual do catálogo já é público. Não cria rascunhos nem fluxo editorial.
-- A autoria não altera conteúdo, ordem, versão, publicação ou congelamento.
CREATE OR REPLACE FUNCTION academy_preservar_autoria() RETURNS TRIGGER LANGUAGE plpgsql AS '
BEGIN
 RAISE EXCEPTION ''Autoria é imutável; novas versões exigem novo vínculo.'' USING ERRCODE=''23514'';
END';
CREATE OR REPLACE TRIGGER academy_autoria_imutavel BEFORE UPDATE OR DELETE ON academy_autoria
 FOR EACH ROW EXECUTE FUNCTION academy_preservar_autoria();
ALTER TABLE academy_disponibilidade ADD COLUMN IF NOT EXISTS tema VARCHAR(160)
 CHECK (tema IS NULL OR length(trim(tema))>0);
