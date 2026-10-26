-- V13: controle do PLR recebido. Registra o dinheiro real (recebimentos) e os gastos feitos com ele, cada gasto
-- opcionalmente ligado a um destino do plano de uso do PLR (destino_adicional, evento PLR = "envelope").
-- Não altera a projeção. Exclusão lógica (deletado_em), como em lancamento.

CREATE TABLE plr_recebimento (
    id          BIGSERIAL PRIMARY KEY,
    data        DATE          NOT NULL,
    descricao   VARCHAR(120)  NOT NULL,
    valor       NUMERIC(14,2) NOT NULL CHECK (valor > 0),
    criado_em   TIMESTAMP     NOT NULL DEFAULT now(),
    deletado_em TIMESTAMP
);

CREATE TABLE plr_gasto (
    id          BIGSERIAL PRIMARY KEY,
    data        DATE          NOT NULL,
    descricao   VARCHAR(160)  NOT NULL,
    valor       NUMERIC(14,2) NOT NULL CHECK (valor > 0),
    destino_id  BIGINT REFERENCES destino_adicional (id) ON DELETE SET NULL,
    criado_em   TIMESTAMP     NOT NULL DEFAULT now(),
    deletado_em TIMESTAMP
);

CREATE INDEX plr_gasto_destino_idx ON plr_gasto (destino_id);
