CREATE TABLE conta_papel (
    conta_id BIGINT NOT NULL REFERENCES conta(id) ON DELETE CASCADE,
    papel VARCHAR(20) NOT NULL,
    PRIMARY KEY (conta_id,papel),
    CONSTRAINT conta_papel_valido CHECK (papel IN ('USUARIA','EDUCADORA','EMPRESA','ADMIN'))
);

-- Contas existentes recebem somente USUARIA.
INSERT INTO conta_papel(conta_id,papel) SELECT id,'USUARIA' FROM conta;

CREATE FUNCTION verificar_combinacao_papeis() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    conta_anterior BIGINT;
    papel_anterior VARCHAR(20);
BEGIN
    IF TG_OP = 'UPDATE' THEN
        conta_anterior := OLD.conta_id;
        papel_anterior := OLD.papel;
    END IF;

    -- Serializa atribuições da mesma conta, inclusive em REPEATABLE READ:
    -- a atualização sem mudança de valor cria uma versão MVCC e evita write skew.
    UPDATE conta SET nome=nome WHERE id=NEW.conta_id;

    IF EXISTS (
        SELECT 1 FROM conta_papel p WHERE p.conta_id=NEW.conta_id
        AND (p.conta_id,p.papel) IS DISTINCT FROM (conta_anterior,papel_anterior)
        AND ((NEW.papel='EMPRESA' AND p.papel IN ('USUARIA','EDUCADORA'))
          OR (NEW.papel IN ('USUARIA','EDUCADORA') AND p.papel='EMPRESA'))
    ) THEN
        RAISE EXCEPTION 'EMPRESA não pode coexistir com USUARIA ou EDUCADORA na mesma conta.'
            USING ERRCODE='23514', CONSTRAINT='conta_papel_combinacao';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER conta_papel_combinacao
BEFORE INSERT OR UPDATE ON conta_papel
FOR EACH ROW EXECUTE FUNCTION verificar_combinacao_papeis();
