ALTER TABLE reservas
    ADD COLUMN cancelada_em TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN cancelada_por_usuario_id UUID;

ALTER TABLE reservas
    ADD CONSTRAINT fk_reservas_cancelada_por_usuario
        FOREIGN KEY (cancelada_por_usuario_id) REFERENCES usuarios (id);

ALTER TABLE reservas
    DROP CONSTRAINT chk_reservas_decisao_registrada;

ALTER TABLE reservas
    ADD CONSTRAINT chk_reservas_decisao_registrada
        CHECK (
            (status = 'SOLICITADA'
                AND decidida_em IS NULL
                AND decidida_por_usuario_id IS NULL
                AND motivo_negacao IS NULL
                AND cancelada_em IS NULL
                AND cancelada_por_usuario_id IS NULL)
            OR (status = 'APROVADA'
                AND decidida_em IS NOT NULL
                AND decidida_por_usuario_id IS NOT NULL
                AND motivo_negacao IS NULL
                AND cancelada_em IS NULL
                AND cancelada_por_usuario_id IS NULL)
            OR (status = 'NEGADA'
                AND decidida_em IS NOT NULL
                AND decidida_por_usuario_id IS NOT NULL
                AND motivo_negacao IS NOT NULL
                AND length(btrim(motivo_negacao)) > 0
                AND cancelada_em IS NULL
                AND cancelada_por_usuario_id IS NULL)
            OR (status = 'CANCELADA'
                AND ((cancelada_em IS NULL AND cancelada_por_usuario_id IS NULL)
                    OR (cancelada_em IS NOT NULL AND cancelada_por_usuario_id IS NOT NULL)))
        );