-- ATENÇÃO: valores de exemplo (iguais aos do protótipo). Os reais vêm da aba "Valores"
-- da planilha e devem ser conferidos/editados em /api/parametros.
INSERT INTO cargo (nome, jornada, usa_delta, ordem) VALUES
 ('Técnico Bancário Novo', 6, TRUE, 0),
 ('Assistente Júnior', 8, FALSE, 1),
 ('Assistente Pleno', 8, FALSE, 2),
 ('Assistente Sênior', 8, FALSE, 3),
 ('Assistente Master TI', 8, FALSE, 4),
 ('Consultor de TI', 8, FALSE, 5),
 ('Supervisor de TI', 8, FALSE, 6),
 ('Coordenador de Projetos TI', 8, FALSE, 7),
 ('Coordenador de TI', 8, FALSE, 8),
 ('Gerente Executivo', 8, FALSE, 9),
 ('Gerente de Centralizadora', 8, FALSE, 10),
 ('Gerente Nacional', 8, FALSE, 11),
 ('Superintendente Nacional', 8, FALSE, 12);

INSERT INTO cargo_valor (cargo_id, piso, gratificacao, vigencia_inicio)
SELECT c.id, v.piso, v.grat, DATE '2026-01-01'
FROM cargo c JOIN (VALUES
 ('Técnico Bancário Novo', 0, 0),
 ('Assistente Júnior', 6200, 1900),
 ('Assistente Pleno', 6200, 2600),
 ('Assistente Sênior', 6200, 3400),
 ('Assistente Master TI', 6200, 4300),
 ('Consultor de TI', 6200, 5200),
 ('Supervisor de TI', 6200, 6300),
 ('Coordenador de Projetos TI', 6200, 7600),
 ('Coordenador de TI', 6200, 8400),
 ('Gerente Executivo', 6200, 11200),
 ('Gerente de Centralizadora', 6200, 13800),
 ('Gerente Nacional', 6200, 17500),
 ('Superintendente Nacional', 6200, 24000)
) AS v(nome, piso, grat) ON v.nome = c.nome;

-- Deltas 201..248 (RH 115 Anexo XIII): progressão de exemplo
INSERT INTO delta_salarial (referencia, valor, vigencia_inicio)
SELECT d, 5200 + (d - 201) * 92, DATE '2026-01-01' FROM generate_series(201, 248) AS d;

INSERT INTO tabela_inss_faixa (ate, aliquota, deducao, vigencia_inicio) VALUES
 (1621.00, 0.07500, 0.00, '2026-01-01'),
 (2902.84, 0.09000, 24.32, '2026-01-01'),
 (4354.27, 0.12000, 111.40, '2026-01-01'),
 (8475.55, 0.14000, 198.49, '2026-01-01');

INSERT INTO tabela_irpf_faixa (ate, aliquota, deducao, vigencia_inicio) VALUES
 (2428.80, 0.00000, 0.00, '2026-01-01'),
 (2826.65, 0.07500, 182.16, '2026-01-01'),
 (3751.05, 0.15000, 394.16, '2026-01-01'),
 (4664.68, 0.22500, 675.49, '2026-01-01'),
 (NULL,    0.27500, 908.73, '2026-01-01');

INSERT INTO parametro_beneficio (
    vigencia_inicio, pai, home_office, vr_va, vt_referencia, vt_percentual,
    sindicato_percentual, sindicato_teto, saude_percentual, saude_teto_percentual,
    plano_por_dependente, inss_teto, inss_teto_salario, ir_deducao_minima,
    ir_limite_isencao, ir_limite_reducao, ir_reducao_a, ir_reducao_b,
    divisor_jornada_6h, divisor_jornada_8h
) VALUES (
    '2026-01-01', 620.00, 1000.00, 1450.00, 5200.00, 0.04000,
    0.01000, 149.21, 0.03500, 0.07000,
    502.08, 988.09, 8475.55, 607.20,
    5000.00, 7350.00, 978.62000, 0.133145,
    180, 220
);

INSERT INTO categoria (nome, ordem) VALUES
 ('Moradia',1),('Casa',2),('Telefone',3),('Reserva',4),('Pessoal',5),
 ('Transporte',6),('Alimentação',7),('Lazer',8),('Saúde',9),('Outros',10);
