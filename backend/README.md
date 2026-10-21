# Backend — Gestão Financeira Box

Java 21 + Spring Boot 3.3, JPA/Hibernate, Flyway, PostgreSQL.

## Rodar

Com Docker, a partir da raiz do projeto (tutorial completo no [README.md](../README.md)):

```bash
cp .env.example .env   # defina DB_PASSWORD
docker compose up -d --build   # db + backend + frontend
```

Sem Docker: tenha um Postgres com o banco `gestafinanceira` e rode

```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5432/gestafinanceira DB_USER=postgres DB_PASSWORD=... mvn spring-boot:run
```

O Flyway cria as tabelas e o seed na primeira subida. API em `http://localhost:8080`, saúde em `/actuator/health`.

## Testes

```bash
DB_PASSWORD=... mvn test
```

Os testes de domínio não precisam de banco. Os de integração usam o banco `gestafinanceira_test`
(variável `TEST_DB_URL`) e fazem rollback a cada teste.

## Endpoints

Meses de 13º e férias são 1..12; competências são `AAAA-MM`; datas de vigência `AAAA-MM-DD`.
Erros: `400` validação, `404` não encontrado, `422` regra de negócio, `409` integridade.

| Recurso | Endpoints |
|---|---|
| Perfil | `GET/PUT /api/perfil` (campo `nome`: opcional, até 60 caracteres, aparado; vazio ou em branco grava `null`; PUT sem `nome` mantém o atual; excedeu → 400) |
| Férias | `GET/PUT /api/ferias` |
| 13º | `GET/PUT /api/decimo` (lista de `{mes, percentual}`; soma ≤ 100), `POST /api/decimo/parcelas` (acrescenta o que falta para 100%) |
| PLR | `GET/PUT /api/plr` (`{ativo, parcelas:[{mes, percentual, valor, valorBruto, desconto, valorLiquido}], totalPercentual, aviso, estimativaAnual}`; no PUT valem `ativo` (omitido mantém), `mes`, `percentual` e `valor` (bruto opcional); soma dos % ≤ 100, meses únicos; ativar sem parcelas grava as padrão set 50% + mar 50%) |
| Bônus Caixa | `GET/PUT /api/bonus` (`{ativo, mes 1..12, salarios}`; só cargos de 8h, `salarios` ≤ máximo vigente) |
| Gastos | `GET/POST /api/lancamentos`, `POST /api/lancamentos/novo` (linha padrão), `GET /api/lancamentos/resumo` (média por categoria), `GET/PUT/DELETE /api/lancamentos/{id}` (o DELETE é lógico: soft delete, ver abaixo) |
| Categorias | `GET/POST /api/categorias`, `DELETE /api/categorias/{nome}` (`Reserva` não é categoria de gasto: a reserva fica em Metas; criar categoria ou gasto com esse nome retorna `422`) |
| Metas | `GET/POST /api/metas`, `POST /api/metas/novo` (meta padrão), `GET/PUT/DELETE /api/metas/{id}` |
| Projeção | `GET /api/projecao?inicio=AAAA-MM` (padrão: mês atual), `GET /api/projecao/mes-comum?mes=AAAA-MM` |
| Agregado | `GET /api/dados` (inclui `bonus` e `plr`), `POST /api/dados/restaurar` (volta ao padrão, com PLR inativo (parcelas set/mar 50%) e bônus inativo; mantém o `nome` do perfil; não toca nos parâmetros) |
| Vigentes | `GET /api/parametros/vigentes?data=` (cargos, deltas, INSS, IRPF e benefícios vigentes) |
| Cargos | `GET /api/parametros/cargos?data=`, `POST`, `GET/PUT/DELETE /api/parametros/cargos/{id}` |
| Deltas | `GET/PUT /api/parametros/deltas`, `DELETE /api/parametros/deltas/{inicio}` |
| INSS | `GET/PUT /api/parametros/inss`, `DELETE /api/parametros/inss/{inicio}` |
| IRPF | `GET/PUT /api/parametros/irpf`, `DELETE /api/parametros/irpf/{inicio}` |
| Benefícios | `GET/PUT /api/parametros/beneficios`, `DELETE /api/parametros/beneficios/{inicio}` |

Tabelas com vigência: o `PUT` grava uma vigência inteira (identificada por `vigenciaInicio`) e o fim das
vigências é recalculado. Para cada mês da projeção vale a vigência mais recente iniciada até o dia 1º do mês.

## Gastos: exclusão lógica e expiração

- **Soft delete.** `lancamento` tem `deletado_em` (timestamp) e `motivo_exclusao` (`USUARIO` ou `EXPIRADO`). `DELETE /api/lancamentos/{id}` e "restaurar padrão" só marcam essas colunas (motivo `USUARIO`); nenhuma linha é apagada. Listagem, resumo por categoria, projeção e `GET /api/dados` ignoram lançamentos com `deletado_em` preenchido, e um id excluído responde `404`.
- **Expiração de gasto pontual.** Um gasto `PONTUAL` "ultrapassou" quando seu mês inicial é **anterior ao mês de hoje** (no próprio mês ele ainda aparece). Nesse caso recebe `deletado_em = agora` e motivo `EXPIRADO`. Recorrentes e parcelados nunca expiram. A varredura é preguiçosa e idempotente: roda (em transação) no início de listagem, busca, resumo, projeção e `/api/dados`, usando um `Clock` injetável (`ConfigWeb.clock()`; os testes o substituem por um relógio fixo). Não há scheduler.
- **Categoria Reserva removida** (migração `V7`): os lançamentos que a usavam (ativos ou excluídos) foram realocados para `Outros`, sem perda de dados, e a categoria foi apagada.

## Fontes dos dados e premissas

Os parâmetros (migração `V4__dados_reais_plr_bonus.sql`) vêm das planilhas do projeto:

- *Tabela Salarial - Caixa Econômica Federal 2026*: remuneração-base (piso) de cada cargo (linha 4 de cada bloco), PLR anual
  estimado (linha 29), VR 1.227,16 + Cesta-Alimentação 967,00 (= `vr_va` 2.194,16), auxílio-creche 729,21 (PAI),
  home office 104,48, teto do sindicato 149,21 e tabela do INSS 2026 (teto 988,09). A aba de 8h tem o *Bônus Caixa (até 3 salários)*,
  guardado como `bonus_caixa_max_salarios = 3`.
- *Simulador de Gastos*: aba *Deltas* (48 valores reais, referências 201..248, vigência 2026-01-01) e aba *Valores*
  (gratificação de cada cargo). Os 13 cargos têm `usa_delta = true`; 6h para os seis primeiros (Técnico a Consultor de TI) e 8h para os demais.

Regras do simulador reproduzidas pelo motor:

- Base = `MAX(piso, delta + gratificação)` para todos os cargos; sobreaviso = `base × dias × 8 ÷ (180 | 220)` (6h | 8h).
- INSS e sindicato incidem sobre o **bruto** (base + sobreaviso). VR/VA, creche, home office, parcelas do 13º, PLR e bônus **não** entram na base do INSS.
- VT = `vt_percentual` (4%) × **valor do delta escolhido no perfil** (a coluna `vt_referencia` foi removida).
- FUNCEF: `funcefPct` deve ser 0 (não participa) ou estar entre 5 e 12 (senão 422).
- **PLR** (`evento_salarial`, tipo `PLR`; flag `perfil.plr_ativo`, padrão **inativo** = nada entra na projeção). Parcelas padrão: setembro 50% (antecipação) e março 50% (saldo); mês, percentual e `valor` são editáveis. Valor **bruto** da parcela = `valor` informado (se houver) ou PLR anual do cargo × percentual. Tributação (`domain/calculo/plr/CalculadoraPlr`, tabela `tabela_plr_faixa` com vigência, V5): **sem INSS**; IR em **tributação exclusiva na fonte** por tabela própria do PLR (2026: isento até 8.214,40; 7,5% − 616,08 até 9.922,28; 15% − 1.360,25 até 13.167,00; 22,5% − 2.347,78 até 16.380,38; 27,5% − 3.166,80 acima), aplicada sobre o **total do PLR do ciclo** (soma das parcelas), descontando o já retido antes (Lei 10.101/2000, art. 3º, com a redação da Lei 12.832/2013). Suposições: (1) a antecipação é paga sem retenção e a parcela de saldo (a de **menor mês**, paga no início do ano seguinte) carrega todo o IR do ciclo; (2) o redutor/isenção da Lei 15.270/2025 (renda até R$ 7.350) **não** se aplica ao PLR, que segue em tabela própria e exclusiva (fontes secundárias: Receita Federal/InfoMoney/guias contábeis de 2026; não foi possível abrir o texto oficial); (3) o IR do ciclo usa a estimativa e a tabela vigentes no mês do saldo; (4) a tributação mínima de altas rendas não é modelada. Se a empresa retiver IR já na antecipação, ajuste o `valor`/percentuais ou a tabela. O mês traz `plr` = **líquido** (entra em receitas), `plrBruto` e `plrDesconto`.
- **Bônus Caixa** (colunas `bonus_*` do perfil): valor = base × salários, **tratado como líquido**, pago no mês configurado; só para cargos de 8h
  (`PUT /api/bonus` e troca de cargo recusam com 422 nos demais). Padrão: inativo.
- Cada mês da projeção traz `plr` (líquido), `plrBruto`, `plrDesconto` e `bonus` (0 quando não há); `evento` lista todos os eventos do mês unidos por " + " (ex.: "13º + PLR").
- **Férias** (líquido, como na planilha): `terço = base/3`; `b2 = base + base/3`; `férias = terço − (INSS(b2) − INSS(base)) − (IRPF(b2) − IRPF(base))`, com IRPF de dedução fixa 607,20 (INSS e FUNCEF não deduzidos). O valor é a receita líquida do mês das férias (sem tributar de novo). Validado ao centavo nos 12 cargos. **Abono** (a planilha não modela): soma-se ao líquido mais base/3, isento — premissa do protótipo, a confirmar.
- **13º**: as parcelas são pagas brutas (percentual × base). Na **última parcela do ano** desconta-se `INSS(total) + IR(total) + FUNCEF (pct × total) + Saúde Caixa sobre o total`, com `total` = soma das parcelas × base. Sem FUNCEF o IR deduz o INSS; **com FUNCEF o IR usa a dedução fixa de 607,20 (INSS e FUNCEF não deduzidos), o que aumenta o IR — premissa replicada da planilha, a confirmar (suspeita).** A saúde não é arredondada na planilha (tolerância de 0,01 nos testes).

## Estrutura

`domain` (cálculo puro, sem Spring) · `application` (serviços) · `infra.persistencia` (JPA) · `api` (REST/DTOs).
