-- V12: plano de uso do 13º e do PLR. Cada linha é uma fatia (percentual do líquido de cada parcela do evento)
-- com um destino: LAZER, OBRIGATORIO ou RESERVA. meta_id (só para RESERVA) liga a fatia a uma meta; ao apagar a
-- meta, a fatia vira reserva geral dos adicionais (ON DELETE SET NULL). A soma dos percentuais por evento (≤ 100)
-- é validada pelo serviço; o que faltar fica "sem destino" (no saldo disponível). Sem linhas = comportamento
-- anterior (todo o adicional fica no saldo).

CREATE TABLE destino_adicional (
    id          BIGSERIAL PRIMARY KEY,
    evento      VARCHAR(10)  NOT NULL CHECK (evento IN ('DECIMO', 'PLR')),
    nome        VARCHAR(60)  NOT NULL CHECK (char_length(btrim(nome)) BETWEEN 1 AND 60),
    tipo        VARCHAR(12)  NOT NULL CHECK (tipo IN ('LAZER', 'OBRIGATORIO', 'RESERVA')),
    percentual  NUMERIC(5,2) NOT NULL CHECK (percentual > 0 AND percentual <= 100),
    meta_id     BIGINT REFERENCES meta (id) ON DELETE SET NULL,
    ordem       INT          NOT NULL DEFAULT 0,
    CHECK (meta_id IS NULL OR tipo = 'RESERVA')
);

CREATE INDEX destino_adicional_evento_idx ON destino_adicional (evento, ordem);
CREATE INDEX destino_adicional_meta_idx ON destino_adicional (meta_id);
