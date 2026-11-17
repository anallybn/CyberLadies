-- CyberChat já publicado na main, integrado à identidade e aprovação atuais.
CREATE VIEW chat_contexto AS
 SELECT 'TRILHA'::text tipo,'TRILHA:'||i.usuaria_id||':'||i.trilha_id chave,
 i.usuaria_id aluna_id,p.educadora_id responsavel_id,t.titulo,
 i.trilha_id,NULL::bigint agendamento_id,NULL::bigint candidatura_id,
 true aberto,e.status='APROVADA' aprovada
 FROM academy_inscricao i JOIN academy_autoria p ON p.trilha_id=i.trilha_id
 JOIN academy_trilha t ON t.id=i.trilha_id JOIN educadora e ON e.conta_id=p.educadora_id
 JOIN conta a ON a.id=i.usuaria_id AND (EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=a.id AND cp.papel='USUARIA') AND NOT EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=a.id AND cp.papel='EDUCADORA'))
 JOIN conta r ON r.id=p.educadora_id AND EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=r.id AND cp.papel='EDUCADORA')
 UNION ALL
 SELECT 'MENTORIA','MENTORIA:'||g.id,g.usuaria_id,m.usuaria_id,
 coalesce(d.tema,'Mentoria — '||m.especialidade),NULL,g.id,NULL,
 g.status='CONFIRMADO',e.status='APROVADA'
 FROM academy_agendamento g JOIN academy_disponibilidade d ON d.id=g.disponibilidade_id
 JOIN academy_mentora m ON m.id=d.mentora_id JOIN educadora e ON e.conta_id=m.usuaria_id
 JOIN conta a ON a.id=g.usuaria_id AND (EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=a.id AND cp.papel='USUARIA') AND NOT EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=a.id AND cp.papel='EDUCADORA'))
 JOIN conta r ON r.id=m.usuaria_id AND EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=r.id AND cp.papel='EDUCADORA')
 UNION ALL
 SELECT 'CANDIDATURA','CANDIDATURA:'||c.id,c.usuaria_id,e.conta_id,
 v.titulo,NULL,NULL,c.id,v.ativa,true
 FROM cyber_candidatura c JOIN cyber_vaga v ON v.id=c.vaga_id
 JOIN conta_empresa e ON e.empresa_id=v.empresa_id
 JOIN conta a ON a.id=c.usuaria_id AND (EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=a.id AND cp.papel='USUARIA') AND NOT EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=a.id AND cp.papel='EDUCADORA'))
 JOIN conta r ON r.id=e.conta_id AND EXISTS(SELECT 1 FROM conta_papel cp WHERE cp.conta_id=r.id AND cp.papel='EMPRESA');
CREATE TABLE chat_conversa (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 contexto_chave TEXT NOT NULL UNIQUE,
 tipo VARCHAR(12) NOT NULL CHECK(tipo IN ('TRILHA','MENTORIA','CANDIDATURA')),
 aluna_id BIGINT NOT NULL REFERENCES conta(id),
 responsavel_id BIGINT NOT NULL REFERENCES conta(id),
 trilha_id BIGINT,
 agendamento_id BIGINT REFERENCES academy_agendamento(id),
 candidatura_id BIGINT REFERENCES cyber_candidatura(id),
 ultima_seq BIGINT NOT NULL DEFAULT 0 CHECK(ultima_seq>=0),
 criado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 atividade_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 CHECK(aluna_id<>responsavel_id),
 CHECK((tipo='TRILHA' AND trilha_id IS NOT NULL AND agendamento_id IS NULL AND candidatura_id IS NULL)
 OR (tipo='MENTORIA' AND trilha_id IS NULL AND agendamento_id IS NOT NULL AND candidatura_id IS NULL)
 OR (tipo='CANDIDATURA' AND trilha_id IS NULL AND agendamento_id IS NULL AND candidatura_id IS NOT NULL)),
 FOREIGN KEY(aluna_id,trilha_id) REFERENCES academy_inscricao(usuaria_id,trilha_id)
);
CREATE TABLE chat_participante (
 conversa_id UUID NOT NULL REFERENCES chat_conversa(id),
 conta_id BIGINT NOT NULL REFERENCES conta(id),
 PRIMARY KEY(conversa_id,conta_id)
);
CREATE INDEX chat_participante_conta_idx ON chat_participante(conta_id,conversa_id);
CREATE INDEX chat_conversa_atividade_idx ON chat_conversa(atividade_em DESC,id);
CREATE TABLE chat_mensagem (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 conversa_id UUID NOT NULL,
 autora_id BIGINT NOT NULL,
 sequencia BIGINT NOT NULL CHECK(sequencia>0),
 texto VARCHAR(2000) NOT NULL CHECK(length(btrim(texto))>0),
 chave_envio UUID NOT NULL,
 enviado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 FOREIGN KEY(conversa_id,autora_id) REFERENCES chat_participante(conversa_id,conta_id),
 UNIQUE(conversa_id,sequencia),
 UNIQUE(conversa_id,autora_id,chave_envio)
);
CREATE INDEX chat_mensagem_autora_data_idx ON chat_mensagem(autora_id,enviado_em);
CREATE TABLE chat_leitura (
 conversa_id UUID NOT NULL,
 conta_id BIGINT NOT NULL,
 ultima_seq BIGINT,
 atualizado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 PRIMARY KEY(conversa_id,conta_id),
 FOREIGN KEY(conversa_id,conta_id) REFERENCES chat_participante(conversa_id,conta_id),
 FOREIGN KEY(conversa_id,ultima_seq) REFERENCES chat_mensagem(conversa_id,sequencia)
);
CREATE FUNCTION chat_travar_contexto(c chat_conversa) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
 IF c.tipo IN ('TRILHA','MENTORIA') THEN
  PERFORM 1 FROM educadora WHERE conta_id=c.responsavel_id FOR SHARE;
 END IF;
 IF c.tipo='MENTORIA' THEN
  PERFORM 1 FROM academy_agendamento WHERE id=c.agendamento_id FOR SHARE;
 ELSIF c.tipo='CANDIDATURA' THEN
  PERFORM 1 FROM cyber_vaga WHERE id=(SELECT vaga_id FROM cyber_candidatura WHERE id=c.candidatura_id) FOR SHARE;
 END IF;
END $$;
CREATE FUNCTION chat_validar_conversa() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' THEN
  IF (NEW.id,NEW.contexto_chave,NEW.tipo,NEW.aluna_id,NEW.responsavel_id,NEW.trilha_id,NEW.agendamento_id,NEW.candidatura_id,NEW.criado_em)
   IS DISTINCT FROM (OLD.id,OLD.contexto_chave,OLD.tipo,OLD.aluna_id,OLD.responsavel_id,OLD.trilha_id,OLD.agendamento_id,OLD.candidatura_id,OLD.criado_em) THEN
   RAISE EXCEPTION 'Identidade da conversa é imutável.' USING ERRCODE='23514';
  END IF;
 ELSE
  PERFORM chat_travar_contexto(NEW);
  IF NOT EXISTS(SELECT 1 FROM chat_contexto x WHERE x.chave=NEW.contexto_chave AND x.tipo=NEW.tipo
   AND x.aluna_id=NEW.aluna_id AND x.responsavel_id=NEW.responsavel_id
   AND x.trilha_id IS NOT DISTINCT FROM NEW.trilha_id AND x.agendamento_id IS NOT DISTINCT FROM NEW.agendamento_id
   AND x.candidatura_id IS NOT DISTINCT FROM NEW.candidatura_id AND x.aberto AND x.aprovada) THEN
   RAISE EXCEPTION 'Contexto indisponível.' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER chat_conversa_validar BEFORE INSERT OR UPDATE ON chat_conversa FOR EACH ROW EXECUTE FUNCTION chat_validar_conversa();
CREATE FUNCTION chat_validar_participante() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP<>'INSERT' OR NOT EXISTS(SELECT 1 FROM chat_conversa c WHERE c.id=NEW.conversa_id AND NEW.conta_id IN(c.aluna_id,c.responsavel_id)) THEN
  RAISE EXCEPTION 'Participantes são definidos pelo contexto e imutáveis.' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER chat_participante_validar BEFORE INSERT OR UPDATE OR DELETE ON chat_participante FOR EACH ROW EXECUTE FUNCTION chat_validar_participante();
CREATE FUNCTION chat_iniciar_participantes() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO chat_participante VALUES(NEW.id,NEW.aluna_id),(NEW.id,NEW.responsavel_id);
 INSERT INTO chat_leitura(conversa_id,conta_id) VALUES(NEW.id,NEW.aluna_id),(NEW.id,NEW.responsavel_id);
 RETURN NEW;
END $$;
CREATE TRIGGER chat_conversa_iniciar AFTER INSERT ON chat_conversa FOR EACH ROW EXECUTE FUNCTION chat_iniciar_participantes();
CREATE FUNCTION chat_validar_mensagem() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE c chat_conversa;
BEGIN
 SELECT * INTO c FROM chat_conversa WHERE id=NEW.conversa_id FOR UPDATE;
 PERFORM chat_travar_contexto(c);
 IF NOT EXISTS(SELECT 1 FROM chat_contexto x WHERE x.chave=c.contexto_chave AND x.aluna_id=c.aluna_id
  AND x.responsavel_id=c.responsavel_id AND x.aberto AND x.aprovada) THEN
  RAISE EXCEPTION 'Novas mensagens bloqueadas.' USING ERRCODE='23514';
 END IF;
 UPDATE chat_conversa SET ultima_seq=ultima_seq+1,atividade_em=clock_timestamp() WHERE id=c.id RETURNING ultima_seq INTO NEW.sequencia;
 NEW.enviado_em=clock_timestamp();
 RETURN NEW;
END $$;
CREATE TRIGGER chat_mensagem_validar BEFORE INSERT ON chat_mensagem FOR EACH ROW EXECUTE FUNCTION chat_validar_mensagem();
CREATE FUNCTION chat_preservar_mensagem() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Mensagens são imutáveis nesta versão.' USING ERRCODE='23514'; END $$;
CREATE TRIGGER chat_mensagem_imutavel BEFORE UPDATE OR DELETE ON chat_mensagem FOR EACH ROW EXECUTE FUNCTION chat_preservar_mensagem();
CREATE FUNCTION chat_validar_leitura() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND ((NEW.conversa_id,NEW.conta_id) IS DISTINCT FROM (OLD.conversa_id,OLD.conta_id)
  OR coalesce(NEW.ultima_seq,0)<coalesce(OLD.ultima_seq,0)) THEN
  RAISE EXCEPTION 'Cursor de leitura inválido.' USING ERRCODE='23514';
 END IF;
 NEW.atualizado_em=clock_timestamp();
 RETURN NEW;
END $$;
CREATE TRIGGER chat_leitura_validar BEFORE INSERT OR UPDATE ON chat_leitura FOR EACH ROW EXECUTE FUNCTION chat_validar_leitura();
