-- V4: dados reais da Tabela Salarial 2026 e do Simulador de Gastos (abas Valores e Deltas), PLR e Bonus Caixa.
-- Idempotente sobre o seed V2: atualiza por nome/referencia (mantem ids) e funciona em banco novo (V1..V4) ou existente.
-- Fontes: "Tabela Salarial - Caixa Economica Federal 2026.xlsx" (Remuneracao-Base linha 4, PLR anual linha 29,
-- VR/Cesta linhas 5-6, Bonus Caixa ate 3 salarios) e "Simulador de Gastos" (Valores: gratificacao, PAI, Home Office; Deltas 201..248).

-- ---- cargos --------------------------------------------------------------------------------------
INSERT INTO cargo (nome, jornada, usa_delta, ordem)
SELECT x.nome, x.jornada, TRUE, x.ordem
FROM (VALUES
 ('Técnico Bancário Novo', 6::smallint, 0),
 ('Assistente Júnior', 6::smallint, 1),
 ('Assistente Pleno', 6::smallint, 2),
 ('Assistente Sênior', 6::smallint, 3),
 ('Assistente Master TI', 6::smallint, 4),
 ('Consultor de TI', 6::smallint, 5),
 ('Supervisor de TI', 8::smallint, 6),
 ('Coordenador de Projetos TI', 8::smallint, 7),
 ('Coordenador de TI', 8::smallint, 8),
 ('Gerente Executivo', 8::smallint, 9),
 ('Gerente de Centralizadora', 8::smallint, 10),
 ('Gerente Nacional', 8::smallint, 11),
 ('Superintendente Nacional', 8::smallint, 12)
) AS x(nome, jornada, ordem)
WHERE NOT EXISTS (SELECT 1 FROM cargo c WHERE c.nome = x.nome);

UPDATE cargo SET jornada = x.jornada, usa_delta = TRUE, ordem = x.ordem
FROM (VALUES
 ('Técnico Bancário Novo', 6::smallint, 0),
 ('Assistente Júnior', 6::smallint, 1),
 ('Assistente Pleno', 6::smallint, 2),
 ('Assistente Sênior', 6::smallint, 3),
 ('Assistente Master TI', 6::smallint, 4),
 ('Consultor de TI', 6::smallint, 5),
 ('Supervisor de TI', 8::smallint, 6),
 ('Coordenador de Projetos TI', 8::smallint, 7),
 ('Coordenador de TI', 8::smallint, 8),
 ('Gerente Executivo', 8::smallint, 9),
 ('Gerente de Centralizadora', 8::smallint, 10),
 ('Gerente Nacional', 8::smallint, 11),
 ('Superintendente Nacional', 8::smallint, 12)
) AS x(nome, jornada, ordem)
WHERE cargo.nome = x.nome;

ALTER TABLE cargo_valor ADD COLUMN plr_anual NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (plr_anual >= 0);

INSERT INTO cargo_valor (cargo_id, piso, gratificacao, plr_anual, vigencia_inicio)
SELECT c.id, x.piso, x.grat, x.plr, DATE '2026-01-01'
FROM cargo c JOIN (VALUES
 ('Técnico Bancário Novo', 4351.71, 0.00, 13055.13),
 ('Assistente Júnior', 8940.83, 3000.49, 23007.22),
 ('Assistente Pleno', 11875.43, 4659.19, 25648.36),
 ('Assistente Sênior', 13682.63, 5115.17, 27274.84),
 ('Assistente Master TI', 15759.66, 5820.06, 29144.17),
 ('Consultor de TI', 18166.12, 6961.06, 30379.98),
 ('Supervisor de TI', 18553.08, 8047.68, 30379.98),
 ('Coordenador de Projetos TI', 21381.01, 9275.49, 30379.98),
 ('Coordenador de TI', 24638.78, 10685.28, 30379.98),
 ('Gerente Executivo', 32730.39, 14193.00, 30904.12),
 ('Gerente de Centralizadora', 32730.39, 14193.00, 30904.12),
 ('Gerente Nacional', 43463.77, 18843.82, 33297.67),
 ('Superintendente Nacional', 57723.71, 25027.83, 36477.63)
) AS x(nome, piso, grat, plr) ON x.nome = c.nome
ON CONFLICT (cargo_id, vigencia_inicio)
DO UPDATE SET piso = EXCLUDED.piso, gratificacao = EXCLUDED.gratificacao, plr_anual = EXCLUDED.plr_anual;

-- ---- deltas reais (aba Deltas, RH 115 Anexo XIII) -------------------------------------------------
INSERT INTO delta_salarial (referencia, valor, vigencia_inicio) VALUES
 (201, 4351.71, DATE '2026-01-01'),
 (202, 4450.02, DATE '2026-01-01'),
 (203, 4555.65, DATE '2026-01-01'),
 (204, 4661.28, DATE '2026-01-01'),
 (205, 4764.82, DATE '2026-01-01'),
 (206, 4874.63, DATE '2026-01-01'),
 (207, 4979.21, DATE '2026-01-01'),
 (208, 5094.25, DATE '2026-01-01'),
 (209, 5214.52, DATE '2026-01-01'),
 (210, 5337.93, DATE '2026-01-01'),
 (211, 5458.20, DATE '2026-01-01'),
 (212, 5581.61, DATE '2026-01-01'),
 (213, 5708.16, DATE '2026-01-01'),
 (214, 5843.07, DATE '2026-01-01'),
 (215, 5976.94, DATE '2026-01-01'),
 (216, 6116.03, DATE '2026-01-01'),
 (217, 6254.08, DATE '2026-01-01'),
 (218, 6396.31, DATE '2026-01-01'),
 (219, 6541.69, DATE '2026-01-01'),
 (220, 6698.56, DATE '2026-01-01'),
 (221, 6851.25, DATE '2026-01-01'),
 (222, 7010.22, DATE '2026-01-01'),
 (223, 7171.28, DATE '2026-01-01'),
 (224, 7334.43, DATE '2026-01-01'),
 (225, 7499.67, DATE '2026-01-01'),
 (226, 7674.32, DATE '2026-01-01'),
 (227, 7857.34, DATE '2026-01-01'),
 (228, 8035.13, DATE '2026-01-01'),
 (229, 8220.25, DATE '2026-01-01'),
 (230, 8410.59, DATE '2026-01-01'),
 (231, 8601.98, DATE '2026-01-01'),
 (232, 8806.96, DATE '2026-01-01'),
 (233, 9007.76, DATE '2026-01-01'),
 (234, 9216.93, DATE '2026-01-01'),
 (235, 9430.28, DATE '2026-01-01'),
 (236, 9650.95, DATE '2026-01-01'),
 (237, 9874.76, DATE '2026-01-01'),
 (238, 10097.52, DATE '2026-01-01'),
 (239, 10339.11, DATE '2026-01-01'),
 (240, 10572.33, DATE '2026-01-01'),
 (241, 10820.19, DATE '2026-01-01'),
 (242, 11069.10, DATE '2026-01-01'),
 (243, 11332.65, DATE '2026-01-01'),
 (244, 11592.01, DATE '2026-01-01'),
 (245, 11855.56, DATE '2026-01-01'),
 (246, 12135.85, DATE '2026-01-01'),
 (247, 12418.22, DATE '2026-01-01'),
 (248, 12704.78, DATE '2026-01-01')
ON CONFLICT (referencia, vigencia_inicio) DO UPDATE SET valor = EXCLUDED.valor;

-- ---- beneficios ----------------------------------------------------------------------------------
ALTER TABLE parametro_beneficio ADD COLUMN bonus_caixa_max_salarios NUMERIC(4,2) NOT NULL DEFAULT 3
    CHECK (bonus_caixa_max_salarios >= 0);

UPDATE parametro_beneficio
SET pai = 729.21, home_office = 104.48, vr_va = 2194.16, sindicato_teto = 149.21
WHERE vigencia_inicio = DATE '2026-01-01';

-- VT passa a ser vt_percentual x valor do delta escolhido no perfil
ALTER TABLE parametro_beneficio DROP COLUMN vt_referencia;

-- ---- PLR como evento salarial (parcelas em % do PLR anual) ---------------------------------------
ALTER TABLE evento_salarial DROP CONSTRAINT IF EXISTS evento_salarial_tipo_check;
ALTER TABLE evento_salarial ADD CONSTRAINT evento_salarial_tipo_check CHECK (tipo IN ('DECIMO_TERCEIRO', 'PLR'));

-- ---- Bonus Caixa no perfil (somente jornada de 8h; validado na aplicacao) -------------------------
ALTER TABLE perfil ADD COLUMN bonus_ativo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE perfil ADD COLUMN bonus_mes SMALLINT NOT NULL DEFAULT 12 CHECK (bonus_mes BETWEEN 1 AND 12);
ALTER TABLE perfil ADD COLUMN bonus_salarios NUMERIC(4,2) NOT NULL DEFAULT 0 CHECK (bonus_salarios >= 0);
