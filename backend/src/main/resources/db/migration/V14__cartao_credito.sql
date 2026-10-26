-- V14: cartão de crédito. Cada cartão tem dia de fechamento e de vencimento; as compras (à vista, parceladas ou
-- assinaturas) geram faturas mensais que entram nos gastos fixos e na projeção. Exclusão lógica (deletado_em):
-- cartão excluído = suas compras deixam de contar. valor = total da compra (assinatura: valor mensal);
-- parcelas >= 2 só em PARCELADA (0 nos demais, normalizado pelo serviço).

CREATE TABLE cartao (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(60)   NOT NULL,
    dia_fechamento  SMALLINT      NOT NULL CHECK (dia_fechamento BETWEEN 1 AND 31),
    dia_vencimento  SMALLINT      NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 31),
    limite          NUMERIC(14,2) CHECK (limite IS NULL OR limite >= 0),
    criado_em       TIMESTAMP     NOT NULL DEFAULT now(),
    deletado_em     TIMESTAMP
);

CREATE TABLE compra_cartao (
    id           BIGSERIAL PRIMARY KEY,
    cartao_id    BIGINT        NOT NULL REFERENCES cartao (id),
    descricao    VARCHAR(160)  NOT NULL,
    categoria    VARCHAR(60)   NOT NULL,
    tipo         VARCHAR(12)   NOT NULL CHECK (tipo IN ('A_VISTA', 'PARCELADA', 'ASSINATURA')),
    data_compra  DATE          NOT NULL,
    valor        NUMERIC(14,2) NOT NULL CHECK (valor > 0),
    parcelas     INTEGER       NOT NULL DEFAULT 0,
    criado_em    TIMESTAMP     NOT NULL DEFAULT now(),
    deletado_em  TIMESTAMP
);

CREATE INDEX compra_cartao_cartao_idx ON compra_cartao (cartao_id);
