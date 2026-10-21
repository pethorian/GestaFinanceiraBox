-- V5: PLR com flag de ativacao, valor bruto opcional por parcela e tabela propria de IR (tributacao exclusiva na fonte).
-- Fonte da tabela: tabela progressiva exclusiva da PLR vigente em 2026 (Lei 10.101/2000 e Lei 12.832/2013; Receita Federal).
-- Faixas sobre o total de PLR do ciclo; imposto = total x aliquota - deducao. Sem INSS.

ALTER TABLE perfil ADD COLUMN plr_ativo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE evento_salarial ADD COLUMN valor NUMERIC(14,2) CHECK (valor IS NULL OR valor >= 0);

-- Quem ja tinha parcelas de PLR cadastradas continua com o PLR ativo; os demais comecam inativos.
UPDATE perfil SET plr_ativo = TRUE WHERE EXISTS (SELECT 1 FROM evento_salarial WHERE tipo = 'PLR');

-- Parcelas padrao (setembro 50% e marco 50%) quando nao ha nenhuma.
INSERT INTO evento_salarial (tipo, mes, percentual)
SELECT 'PLR', x.mes, 50 FROM (VALUES (9), (3)) AS x(mes)
WHERE NOT EXISTS (SELECT 1 FROM evento_salarial WHERE tipo = 'PLR');

CREATE TABLE tabela_plr_faixa (
    id               BIGSERIAL PRIMARY KEY,
    ate              NUMERIC(12,2),            -- NULL = sem limite superior
    aliquota         NUMERIC(7,5)  NOT NULL,
    deducao          NUMERIC(12,2) NOT NULL,
    vigencia_inicio  DATE          NOT NULL,
    vigencia_fim     DATE
);

INSERT INTO tabela_plr_faixa (ate, aliquota, deducao, vigencia_inicio) VALUES
 (8214.40,  0.00000,    0.00, '2026-01-01'),
 (9922.28,  0.07500,  616.08, '2026-01-01'),
 (13167.00, 0.15000, 1360.25, '2026-01-01'),
 (16380.38, 0.22500, 2347.78, '2026-01-01'),
 (NULL,     0.27500, 3166.80, '2026-01-01');
