-- Estado inicial do usuário (equivale a dadosIniciais() do frontend).
INSERT INTO perfil (id, cargo_id, delta, dias_sobreaviso, filhos, home_office, vr_va, saude, dependentes,
                    funcef_pct, vale_transporte, sindicato, saldo_inicial, ferias_ativo, ferias_mes, ferias_abono)
SELECT 1, c.id, 228, 0, 1, TRUE, FALSE, 'DEP', 2, 6, TRUE, TRUE, 3200, TRUE, 7, FALSE
FROM cargo c WHERE c.nome = 'Técnico Bancário Novo';

INSERT INTO lancamento (descricao, categoria, tipo, valor, mes_inicial, parcelas)
SELECT d, c, t, v, date_trunc('month', current_date + (off || ' month')::interval)::date, p
FROM (VALUES
 ('Aluguel', 'Moradia', 'RECORRENTE', 2200, 0, 0),
 ('Condomínio e contas', 'Casa', 'RECORRENTE', 850, 0, 0),
 ('Telefone e internet', 'Telefone', 'RECORRENTE', 190, 0, 0),
 ('Mercado e delivery', 'Alimentação', 'RECORRENTE', 1500, 0, 0),
 ('Gastos pessoais', 'Pessoal', 'RECORRENTE', 900, 0, 0),
 ('Notebook', 'Pessoal', 'PARCELADO', 480, 1, 10),
 ('Viagem de fim de ano', 'Lazer', 'PONTUAL', 3200, 2, 0)
) AS x(d, c, t, v, off, p);

INSERT INTO evento_salarial (tipo, mes, percentual) VALUES
 ('DECIMO_TERCEIRO', 2, 50),
 ('DECIMO_TERCEIRO', 11, 50);

INSERT INTO meta (nome, valor_alvo, valor_atual, aporte_mensal) VALUES
 ('Reserva de emergência', 30000, 3200, 900),
 ('Entrada do apartamento', 80000, 12000, 1500),
 ('Troca de carro', 25000, 0, 600);
