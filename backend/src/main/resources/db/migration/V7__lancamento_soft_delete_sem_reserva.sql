-- Soft delete de lançamentos: a linha nunca é apagada; deletado_em marca a exclusão.
-- motivo_exclusao: 'USUARIO' (DELETE na API) ou 'EXPIRADO' (gasto pontual cujo mês já passou).
ALTER TABLE lancamento ADD COLUMN deletado_em TIMESTAMP;
ALTER TABLE lancamento ADD COLUMN motivo_exclusao VARCHAR(10)
    CHECK (motivo_exclusao IS NULL OR motivo_exclusao IN ('USUARIO', 'EXPIRADO'));
CREATE INDEX idx_lancamento_ativos ON lancamento (id) WHERE deletado_em IS NULL;

-- "Reserva" deixa de ser categoria de gasto (a reserva tem aba própria em Metas).
-- Os lançamentos que a usavam (inclusive os já excluídos) são realocados para 'Outros'
-- (a categoria genérica do seed), nunca apagados.
INSERT INTO categoria (nome, ordem)
SELECT 'Outros', COALESCE((SELECT MAX(ordem) FROM categoria), 0) + 1
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nome = 'Outros');

UPDATE lancamento SET categoria = 'Outros' WHERE categoria = 'Reserva';
DELETE FROM categoria WHERE nome = 'Reserva';
