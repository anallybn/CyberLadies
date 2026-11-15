ALTER TABLE educadora
    ADD COLUMN decidida_por BIGINT REFERENCES conta(id),
    ADD COLUMN decidida_em TIMESTAMP WITH TIME ZONE,
    ADD COLUMN motivo_rejeicao VARCHAR(1000),
    ADD CONSTRAINT educadora_decisao_coerente CHECK (
        -- Preserva decisões legadas sem inventar responsável/data.
        (decidida_por IS NULL AND decidida_em IS NULL AND motivo_rejeicao IS NULL)
        OR (decidida_por IS NOT NULL AND decidida_em IS NOT NULL AND (
            (status='APROVADA' AND motivo_rejeicao IS NULL)
            OR (status='REJEITADA' AND motivo_rejeicao IS NOT NULL AND length(btrim(motivo_rejeicao))>0)
        ))
    );

CREATE INDEX educadora_status_conta_idx ON educadora(status,conta_id);
