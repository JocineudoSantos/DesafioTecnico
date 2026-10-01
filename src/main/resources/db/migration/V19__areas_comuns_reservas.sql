CREATE TABLE areas_comuns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_areas_comuns_ativa_nome ON areas_comuns (ativa, nome);

CREATE TABLE reservas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_comum_id UUID NOT NULL,
    morador_id UUID NOT NULL,
    inicio TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    fim TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SOLICITADA',
    criada_em TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservas_area_comum
        FOREIGN KEY (area_comum_id) REFERENCES areas_comuns (id),
    CONSTRAINT fk_reservas_morador
        FOREIGN KEY (morador_id) REFERENCES moradores (id),
    CONSTRAINT chk_reservas_intervalo
        CHECK (fim > inicio),
    CONSTRAINT chk_reservas_status
        CHECK (status IN ('SOLICITADA', 'APROVADA', 'NEGADA', 'CANCELADA'))
);

CREATE INDEX idx_reservas_morador_inicio ON reservas (morador_id, inicio DESC);
CREATE INDEX idx_reservas_area_status_inicio_fim ON reservas (area_comum_id, status, inicio, fim);
