-- V10: Saúde financeira (método 50/30/20 opcional).
-- categoria.grupo classifica cada categoria de gasto em NECESSIDADE ou DESEJO; categorias criadas
-- pelo usuário sem grupo informado ficam como NECESSIDADE. perfil.metodo_50_30_20 liga/desliga a
-- participação no método (o cálculo é sempre feito; a flag só indica a adesão).

ALTER TABLE categoria ADD COLUMN grupo VARCHAR(12) NOT NULL DEFAULT 'NECESSIDADE'
    CHECK (grupo IN ('NECESSIDADE', 'DESEJO'));

UPDATE categoria SET grupo = 'DESEJO' WHERE nome IN ('Pessoal', 'Lazer', 'Outros');

ALTER TABLE perfil ADD COLUMN metodo_50_30_20 BOOLEAN NOT NULL DEFAULT FALSE;
