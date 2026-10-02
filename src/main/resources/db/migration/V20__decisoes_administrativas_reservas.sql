ALTER TABLE reservas
    ADD COLUMN decidida_em TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN decidida_por_usuario_id UUID,
    ADD COLUMN motivo_negacao TEXT;

ALTER TABLE reservas
    ADD CONSTRAINT fk_reservas_decidida_por_usuario
        FOREIGN KEY (decidida_por_usuario_id) REFERENCES usuarios (id),
    ADD CONSTRAINT chk_reservas_decisao_registrada
        CHECK (
            (status = 'SOLICITADA'
                AND decidida_em IS NULL
                AND decidida_por_usuario_id IS NULL
                AND motivo_negacao IS NULL)
            OR (status = 'APROVADA'
                AND decidida_em IS NOT NULL
                AND decidida_por_usuario_id IS NOT NULL
                AND motivo_negacao IS NULL)
            OR (status = 'NEGADA'
                AND decidida_em IS NOT NULL
                AND decidida_por_usuario_id IS NOT NULL
                AND motivo_negacao IS NOT NULL
                AND length(btrim(motivo_negacao)) > 0)
            OR status = 'CANCELADA'
        );
