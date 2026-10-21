-- V11: saldo atual acumulado na FUNCEF (previdência privada). Opcional; usado como ponto de partida da
-- previdência na projeção (contribuições do participante somam a ele, sem rendimento).
ALTER TABLE perfil ADD COLUMN saldo_funcef NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (saldo_funcef >= 0);
