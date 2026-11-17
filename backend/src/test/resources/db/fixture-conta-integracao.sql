-- Apenas fixtures das suítes portadas da origem; nunca é carregado pela aplicação.
CREATE OR REPLACE FUNCTION fixture_conta_integracao(
 nome_fixture TEXT, email_fixture TEXT, hash_fixture TEXT, competencias_fixture TEXT,
 tipo_fixture TEXT DEFAULT 'CYBERLADIE', membro_fixture BOOLEAN DEFAULT false)
RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE nova BIGINT;
BEGIN
 IF current_database() NOT IN ('cyberladies_auth_test','cyberladies_auth_chat_test') THEN RAISE EXCEPTION 'Fixture exige banco isolado.'; END IF;
 INSERT INTO conta(nome,email,senha_hash) VALUES (nome_fixture,email_fixture,hash_fixture) RETURNING id INTO nova;
 IF tipo_fixture IN ('CYBERLADIE','EDUCADORA') THEN
  INSERT INTO usuaria VALUES (nova,competencias_fixture);
  INSERT INTO conta_papel VALUES (nova,'USUARIA');
  IF tipo_fixture='EDUCADORA' THEN INSERT INTO conta_papel VALUES (nova,'EDUCADORA'); END IF;
 ELSE
  INSERT INTO conta_papel VALUES (nova,tipo_fixture);
 END IF;
 IF membro_fixture THEN INSERT INTO talk_participacao VALUES (nova); END IF;
 RETURN nova;
END $$;
@@@
