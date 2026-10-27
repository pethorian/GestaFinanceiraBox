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
| Perfil | `GET/PUT /api/perfil` (campo `nome`: opcional, até 60 caracteres, aparado; vazio ou em branco grava `null`; PUT sem `nome` mantém o atual; excedeu → 400; `saldoFuncef`: saldo atual acumulado na FUNCEF, opcional, ≥ 0, padrão 0, migração `V11`; PUT sem `saldoFuncef` mantém o atual; negativo → 400) |
| Férias | `GET/PUT /api/ferias` |
| 13º | `GET/PUT /api/decimo`: `GET` devolve `{ativo, parcelas:[{mes, percentual}], totalPercentual}` (parcelas fixas e somente leitura: fev 50% e nov 50%); `PUT` aceita apenas `{ativo}` (obrigatório). `POST /api/decimo/parcelas` e a edição de parcelas foram removidos (404/405) |
| PLR | `GET/PUT /api/plr` (`{ativo, parcelas:[{mes, percentual, valor, valorBruto, desconto, valorLiquido}], totalPercentual, aviso, estimativaAnual}`; no PUT valem `ativo` (omitido mantém), `mes`, `percentual` e `valor` (bruto opcional); soma dos % ≤ 100, meses únicos; ativar sem parcelas grava as padrão set 50% + mar 50%) |
| Bônus Caixa | `GET/PUT /api/bonus` (`{ativo, mes 1..12, salarios}`; só cargos de 8h, `salarios` ≤ máximo vigente) |
| Gastos | `GET/POST /api/lancamentos`, `POST /api/lancamentos/novo[?tipo=RECORRENTE|PARCELADO|PONTUAL]` (linha padrão; padrão RECORRENTE), `GET /api/lancamentos/resumo[?inicio=AAAA-MM&grupo=FIXOS|EVENTUAIS]` (média por categoria; FIXOS = RECORRENTE+PARCELADO, EVENTUAIS = PONTUAL; sem `grupo`, soma tudo; valor inválido → 400), `GET/PUT/DELETE /api/lancamentos/{id}` (o DELETE é lógico: soft delete, ver abaixo) |
| Cartão de crédito | `GET/POST /api/cartoes`, `PUT/DELETE /api/cartoes/{id}`, `GET/POST /api/cartoes/{id}/compras`, `PUT/DELETE /api/cartoes/compras/{compraId}`, `GET /api/cartoes/{id}/faturas[?inicio=AAAA-MM]`: ver "Cartão de crédito" |
| Categorias | `GET/POST /api/categorias` (saída e entrada com `grupo` `NECESSIDADE`/`DESEJO`; no POST é opcional e o padrão é `NECESSIDADE`), `PUT /api/categorias/{nome}/grupo` (`{grupo}`; 404 categoria inexistente, 400 grupo inválido), `DELETE /api/categorias/{nome}` (`Reserva` não é categoria de gasto: a reserva fica em Metas; criar categoria ou gasto com esse nome retorna `422`) |
| Metas | `GET/POST /api/metas`, `POST /api/metas/novo` (meta padrão), `GET/PUT/DELETE /api/metas/{id}`. Entrada: `{nome, valorAlvo, valorAtual, aporteMensal, banco}`; `banco` é texto livre opcional (até 80 caracteres depois de aparado; vazio/branco ou ausente grava `null`; excedeu → 400). Saída acrescenta `falta`, `percentual`, `mesesParaAtingir`, `previsao`, `guardadoFimProjecao` e `fimProjecao` (ver "Metas e aportes") |
| Projeção | `GET /api/projecao?inicio=AAAA-MM` (padrão: mês atual), `GET /api/projecao/mes-comum?mes=AAAA-MM`. Cada mês traz `aportes`, `sobra` (já sem os aportes), `acumulado` (disponível em conta), `guardado`, `patrimonio` e `metas` (`[{id, nome, aporte, guardado, aporteAdicional}]`), e no fim `adicionais`, `gastosAdicionais`, `reservasAdicionais` e `guardadoReservaAdicionais` (ver "Plano de uso do 13º e do PLR"); o `resumo` traz `aportes`, `guardadoInicial`, `guardadoFinal` e `patrimonioFinal`. Previdência FUNCEF: cada mês traz também `funcefDecimo`, `previdencia` e `patrimonioTotal` (= patrimonio + previdencia; inclui a reserva dos adicionais, que está em `guardado`), e o `resumo` traz `previdenciaInicial`, `previdenciaFinal` e `patrimonioTotalFinal` (ver "Previdência FUNCEF"). Plano do 13º/PLR: depois dos campos da FUNCEF, cada mês traz `adicionais`, `gastosAdicionais`, `reservasAdicionais` e `guardadoReservaAdicionais`, e o `resumo` traz `gastosAdicionais`, `reservasAdicionais` e `adicionais` (`{decimo, plr, total}`, cada um `{liquido, lazer, obrigatorio, reserva, semDestino}`) |
| Plano do 13º/PLR | `GET/PUT /api/adicionais/{evento}/destinos` (`evento` = `decimo` ou `plr`; outro → 404). PUT recebe a **lista inteira** `[{id?, nome, tipo, percentual, metaId?}]` (vazia = sem destinos; `id` opcional preserva o destino: id do mesmo evento atualiza, ausente insere, destino que não veio é apagado; a ordem recebida é mantida) e devolve o mesmo objeto do GET: `{evento, ativo, destinos:[{id, nome, tipo, percentual, metaId, metaNome}], totalPercentual, semDestinoPercentual, previa:[{mes, liquido, destinos:[{id, nome, tipo, metaId, percentual, valor}], semDestino}]}`. 400: nome vazio ou > 60 (aparado), tipo ausente/inválido, percentual ausente, ≤ 0, > 100 ou com mais de 2 casas, mais de 20 itens, corpo que não é lista; 422: soma > 100, `metaId` com tipo ≠ `RESERVA` ou meta inexistente, `id` desconhecido/de outro evento/repetido, remover destino do PLR que tenha gasto ativo no controle do PLR ("Há gastos do PLR no destino 'X': mova ou apague esses gastos antes.") |
| Controle do PLR | `GET /api/plr/controle` → `{recebido, gasto, saldo, destinos:[{id, nome, tipo, percentual, alocado, gasto, saldo, percentualUsado}], semDestino:{alocado, gasto, saldo, percentualUsado}, recebimentos:[{id, data, descricao, valor}], gastos:[{id, data, descricao, valor, destinoId, destinoNome}]}` (listas por data decrescente). `POST /api/plr/recebimentos` (201), `PUT/DELETE /api/plr/recebimentos/{id}` com `{data, descricao (1..120), valor}`; `POST /api/plr/gastos` (201), `PUT/DELETE /api/plr/gastos/{id}` com `{data, descricao (1..160), valor, destinoId?}`; DELETE → 204, exclusão lógica. 400: data ausente, descrição vazia/longa, valor ausente, ≤ 0 ou com mais de 2 casas; 404: id inexistente/excluído; 422: `destinoId` que não é destino do plano de uso do PLR. Ver "Controle do PLR recebido" |
| Saúde financeira | `GET /api/saude-financeira`, `PUT /api/saude-financeira` (`{participa}`): ver "Saúde financeira (50/30/20)" |
| Agregado | `GET /api/dados` (inclui `bonus`, `plr`, `saudeFinanceira` e `adicionais: {decimo, plr}` com o objeto de `/api/adicionais/{evento}/destinos`), `POST /api/dados/restaurar` (volta ao padrão, com 13º ativo, PLR inativo (parcelas set/mar 50%), bônus inativo e **nenhum destino** no plano do 13º/PLR; `participa` do 50/30/20 volta a falso e as categorias voltam ao grupo padrão; mantém o `nome` do perfil; não toca nos parâmetros) |
| Vigentes | `GET /api/parametros/vigentes?data=` (cargos, deltas, INSS, IRPF e benefícios vigentes) |
| Cargos | `GET /api/parametros/cargos?data=`, `POST`, `GET/PUT/DELETE /api/parametros/cargos/{id}` |
| Deltas | `GET/PUT /api/parametros/deltas`, `DELETE /api/parametros/deltas/{inicio}` |
| INSS | `GET/PUT /api/parametros/inss`, `DELETE /api/parametros/inss/{inicio}` |
| IRPF | `GET/PUT /api/parametros/irpf`, `DELETE /api/parametros/irpf/{inicio}` |
| Benefícios | `GET/PUT /api/parametros/beneficios`, `DELETE /api/parametros/beneficios/{inicio}` |

Tabelas com vigência: o `PUT` grava uma vigência inteira (identificada por `vigenciaInicio`) e o fim das
vigências é recalculado. Para cada mês da projeção vale a vigência mais recente iniciada até o dia 1º do mês.

## Metas e aportes

Regra única em `domain/calculo/metas/CalculadoraAportes` (usada pela projeção e pela previsão da aba Metas):

- **Aporte do mês.** Com alvo (`valor_alvo > 0`): aporta `aporte_mensal` enquanto o guardado não atinge o alvo; no mês em que atinge, aporta **só o que falta**; depois, zero. Se `valor_atual ≥ valor_alvo`, nunca aporta. **Sem alvo (`valor_alvo = 0`): aporta todo mês** (reserva contínua), sem previsão de conclusão. `aporte_mensal = 0`: nunca aporta.
- **Linha do tempo.** `valor_atual` é o saldo guardado no início do 1º mês da projeção, e o 1º mês já tem aporte. O guardado de cada meta evolui mês a mês (`guardado = guardado anterior + aporte`), em `BigDecimal` HALF_UP com 2 casas.
- **Na projeção** (`ProjetorMensal`): `aportes` = soma dos aportes do mês (saída da conta); `sobra = líquido − gastos − aportes`; `acumulado` = saldo inicial + sobras, ou seja, o **dinheiro disponível em conta**; `guardado` = soma dos saldos das metas ao fim do mês; `patrimonio = acumulado + guardado` (o aporte sai da conta, mas continua sendo da pessoa). Meses no vermelho, melhor/pior mês e médias usam a `sobra` já sem os aportes. O mês comum (`/mes-comum`) aplica o aporte do 1º mês a partir dos valores atuais.
- **Previsão** (aba Metas, a contar do mês atual): `mesesParaAtingir` = número de aportes que faltam, `ceil(falta ÷ aporte mensal)`, contando o mês atual (0 = já atingida; `null` sem alvo ou sem aporte); `previsao` = mês do último aporte = mês atual + n − 1 (o mês atual quando já atingida), que é o mesmo mês em que a projeção iniciada hoje atinge o alvo. Ex.: alvo 10.000, atual 1.000, aporte 1.500 em out/26 → 6 aportes, previsão mar/27; com aporte 2.000 → 5 aportes (o último de 1.000), previsão fev/27. `guardadoFimProjecao` é o saldo previsto da meta em `fimProjecao` (12º mês).
- **Banco** (`meta.banco`, migração `V9`): onde o dinheiro está guardado, texto livre informativo; não entra no cálculo.

## Saúde financeira (50/30/20)

Regra em `domain/calculo/saude/Metodo503020` (cálculo puro, HALF_UP, 2 casas; percentuais com 1 casa); entradas montadas por `SaudeFinanceiraService`. O cálculo é **sempre** feito (a tela mostra prévia); `perfil.metodo_50_30_20` (migração `V10`, padrão falso) só registra a adesão.

- **Renda de referência** = líquido do "mês comum" do mês atual (`ProjecaoService.mesComum`: salário recorrente, sem 13º, férias, PLR nem bônus) **+ a contribuição FUNCEF do mesmo mês** (a FUNCEF sai da renda e volta como poupança, para o percentual não ficar distorcido).
- **Gastos** = médias mensais do resumo de lançamentos (mesma base de `GET /api/lancamentos/resumo`, ignorando excluídos) somadas por `categoria.grupo`. A V10 cria `categoria.grupo` (`NECESSIDADE`/`DESEJO`): Pessoal, Lazer e Outros são `DESEJO`; as demais do seed, e as criadas pelo usuário sem grupo, `NECESSIDADE`. "Restaurar padrão" regrupa tudo ao mapeamento padrão.
- **Poupança** = média mensal dos aportes em metas nos 12 meses da projeção (`aportes` / 12) **+ a contribuição FUNCEF do participante no mês comum** (dinheiro guardado para a aposentadoria). A **contrapartida da patrocinadora (Caixa) não entra**: não é renda do usuário, então não aparece na renda nem na poupança. Sem FUNCEF (`funcefPct = 0`) nada muda. `poupancaDetalhe` = `{metas, funcef}`; `atual.poupanca` = `metas + funcef`.
- **Recomendado** = 50% / 30% / 20% da renda. **Status**: necessidades e desejos `ACIMA` se passam do recomendado, senão `OK`; poupança `ABAIXO` se é menor que o recomendado, senão `OK`. `sobraLivre` = renda − necessidades − desejos − poupança (pode ser negativa); `aporteSugerido` = max(0, recomendado.poupanca − atual.poupanca). Renda ≤ 0: recomendado e aporte sugerido zerados, `percentuais` = `null` e um único diagnóstico explicativo.
- **Resposta** (`GET`/`PUT /api/saude-financeira`; o mesmo objeto vem em `GET /api/dados` → `saudeFinanceira`): `{participa, rendaMensal, recomendado:{necessidades,desejos,poupanca}, atual:{...}, percentuais:{...}|null, diferenca:{...} (atual − recomendado), status:{necessidades,desejos,poupanca: "OK"|"ACIMA"|"ABAIXO"}, sobraLivre, aporteSugerido, categorias:[{nome, grupo, mediaMensal}] (todas as categorias, na ordem de cadastro), diagnostico:[texto pt-BR], poupancaDetalhe:{metas, funcef}}`. Com FUNCEF, o diagnóstico inclui "Sua contribuição FUNCEF já conta como poupança: R$ X/mês."

- **Bloco `adicionais`** (no fim do objeto; regra em `domain/calculo/saude/SaudeAdicionais`): como o 13º e o PLR dos 12 meses da projeção (a partir do mês atual, só eventos ativos) são usados, **à parte** do 50/30/20 mensal — os adicionais não entram na renda de referência (mês comum) nem na poupança mensal acima. `{totalLiquidoPeriodo, divisao:{necessidades, desejos, poupanca, semDestino} (OBRIGATORIO → necessidades, LAZER → desejos, RESERVA → poupança), percentuais:{...}|null (do total líquido dos adicionais, 1 casa), comparacao:{recomendado, diferenca, status}|null (50/30/20 do total, status como no método), definido (há algum destino), diagnostico:[...]}`. `percentuais` e `comparacao` são `null` quando não há adicional no período. As frases de `adicionais.diagnostico` (1 a 3: divisão, comparação com os 20% e "ainda não definiu" para o evento sem plano) também são acrescentadas no fim do `diagnostico` principal; use um ou outro na tela.

## Controle do PLR recebido

Registra o dinheiro **real** de PLR e os gastos feitos com ele; **não altera a projeção**. Os destinos do plano de uso do PLR (`destino_adicional`, evento `PLR`) são os "envelopes": uma só fonte de percentuais. Tabelas `plr_recebimento` e `plr_gasto` (migração `V13`; `plr_gasto.destino_id` → `destino_adicional` com `ON DELETE SET NULL`; exclusão lógica por `deletado_em`, como em `lancamento`). Cálculo em `domain/calculo/plr/ControlePlr`: `recebido` = soma dos recebimentos; por destino `alocado = arredondar(recebido × percentual / 100)`, `gasto`, `saldo = alocado − gasto` (negativo = estourou), `percentualUsado = gasto / alocado × 100` (2 casas; 0 se `alocado` = 0); bloco `semDestino`: `alocado = recebido − soma dos alocados` (nunca negativo) e os gastos sem destino. Ex.: recebido 10.000, Gastos 80% e Pessoal 20%, gasto pessoal 350 → Pessoal alocado 2.000,00, saldo 1.650,00, usado 17,50%. Como o PUT dos destinos preserva os ids, o vínculo dos gastos sobrevive à edição do plano; remover um destino com gasto ativo é bloqueado (422). `POST /api/dados/restaurar` exclui logicamente recebimentos e gastos.

## Plano de uso do 13º e do PLR

Tabela `destino_adicional` (migração `V12`): `evento` (`DECIMO`/`PLR`), `nome` (1..60), `tipo` (`LAZER`/`OBRIGATORIO`/`RESERVA`), `percentual` (0 < p ≤ 100, 2 casas), `meta_id` (opcional, só com `RESERVA`; FK `ON DELETE SET NULL`) e `ordem`. Soma por evento ≤ 100; o que faltar fica **sem destino** (no saldo disponível). Padrão: nenhum destino (comportamento anterior). Regras no domínio (`domain/calculo/adicionais`: `CalculadoraDestinos`, `PlanoAdicionais`, `TotaisAdicionais`):

- **Base.** O **líquido de cada parcela**, só com o evento ativo e no mês em que ela cai: 13º = parcela − desconto do 13º daquela parcela (o desconto cai na última, novembro); PLR = líquido da parcela (`plr`). Mês comum (`/mes-comum`) não tem adicionais.
- **Divisão** (HALF_UP, 2 casas): cada destino = líquido × % ÷ 100 arredondado; o **último** destino recebe o resíduo de centavos para que a soma feche em `round(líquido × soma dos % ÷ 100)`; `semDestino` = líquido − soma. Ex.: 1.000,01 com 33,33/33,33/33,34% → 333,30 + 333,30 + 333,41. Líquido ≤ 0: destinos zerados. Caso degenerado (o último ficaria negativo, só com poucos centavos): arredondamento acumulado.
- **Na projeção.** `LAZER` + `OBRIGATORIO` = `gastosAdicionais` (saída do mês, à parte de `gastos`); `RESERVA` = `reservasAdicionais` (sai do disponível e entra em `guardado`). `sobra = líquido − gastos − aportes − gastosAdicionais − reservasAdicionais`. Reserva **ligada a uma meta**: soma no guardado daquela meta (`metas[].aporteAdicional`), **antes** do aporte mensal do mês; o aporte mensal é calculado sobre o guardado já com o extra (`CalculadoraAportes.aportarMes` com extras), então aporta só o que falta ou zero se o extra cobriu o alvo; depois do alvo os aportes mensais param, como sempre. O extra não é limitado ao alvo. Efeito: o aporte extra **antecipa** a meta. Ex.: alvo 10.000, atual 1.000, aporte 1.500/mês (atingiria em mar/27); PLR líquido de 3.000 em nov/26 100% para a meta → out 2.500, nov 2.500 + 3.000 + 1.500 = 7.000, dez 8.500, jan/27 10.000 (atingida); fev/27 em diante sem aporte. A previsão da aba Metas (`mesesParaAtingir`/`previsao`) continua considerando só os aportes mensais. Reserva **sem meta** (ou com a meta apagada): vai para a "Reserva dos adicionais", saldo acumulado mês a mês em `guardadoReservaAdicionais` e incluído em `guardado`/`patrimonio`.
- **Prévia** (`previa` do GET/PUT): as parcelas do evento nos 12 meses a partir do mês atual, mesmo com o evento inativo (`ativo` indica se entra na projeção).
- **Meta apagada:** o banco anula `meta_id` (a fatia continua como `RESERVA`, agora na reserva dos adicionais).

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
- **Previdência FUNCEF** (`perfil.saldo_funcef`, V11; `ProjetorMensal`): a contribuição do participante é dinheiro guardado, não gasto. `previdencia` de cada mês = `saldoFuncef` + contribuições acumuladas do participante até o fim do mês, **sem rendimento**. A contribuição de cada mês é `funcef` (pct × base) mais, no mês da última parcela do 13º, `funcefDecimo` (pct × total do 13º), pois o cálculo do 13º já desconta FUNCEF nessa parcela (`descontoDecimo` a contém; `funcefDecimo` a expõe separada). A contrapartida da patrocinadora não entra. Decisão: `patrimonio` (= acumulado + guardado) **não muda**; `patrimonioTotal` = `patrimonio` + `previdencia` (e `patrimonioTotalFinal` no resumo). O mês isolado (`/mes-comum`) traz `saldoFuncef` + a contribuição daquele mês.
- **Férias** (líquido, como na planilha): `terço = base/3`; `b2 = base + base/3`; `férias = terço − (INSS(b2) − INSS(base)) − (IRPF(b2) − IRPF(base))`, com IRPF de dedução fixa 607,20 (INSS e FUNCEF não deduzidos). O valor é a receita líquida do mês das férias (sem tributar de novo). Validado ao centavo nos 12 cargos. **Abono** (a planilha não modela): soma-se ao líquido mais base/3, isento — premissa do protótipo, a confirmar.
- **13º** (flag `perfil.decimo_ativo`, padrão **ativo**; migração `V8`): parcelas **fixas** em constante no código, fevereiro 50% e novembro 50%, não editáveis. Inativo = nenhum 13º na projeção (nem valor nem desconto). A V8 apaga as linhas `DECIMO_TERCEIRO` de `evento_salarial` (ficam mortas, pois as parcelas deixaram de ser dados) e restringe o `CHECK` da tabela a `PLR`, único uso restante. "Restaurar padrão" volta a ativo. As parcelas são pagas brutas (percentual × base). Na **última parcela do ano** desconta-se `INSS(total) + IR(total) + FUNCEF (pct × total) + Saúde Caixa sobre o total`, com `total` = soma das parcelas × base. Sem FUNCEF o IR deduz o INSS; **com FUNCEF o IR usa a dedução fixa de 607,20 (INSS e FUNCEF não deduzidos), o que aumenta o IR — premissa replicada da planilha, a confirmar (suspeita).** A saúde não é arredondada na planilha (tolerância de 0,01 nos testes).

## Cartão de crédito

Migração `V14` (`cartao`, `compra_cartao`); exclusão lógica por `deletado_em` (cartão excluído = suas compras deixam de contar; "restaurar padrão" exclui todos). Cálculo puro em `domain/calculo/cartao/CalculadoraFatura`.

- **Cartão** (`POST`/`PUT`): `{nome (1..60), diaFechamento (1..31), diaVencimento (1..31), limite? (>= 0, até 2 casas)}`. `GET /api/cartoes` devolve também `faturaAtual` (fatura aberta, que recebe as compras de hoje, pelo `Clock`), `proximaFatura` (a seguinte), `comprometido` (soma das parcelas das faturas ainda por pagar, com vencimento hoje ou depois, sem assinaturas) e `limiteDisponivel` (limite − comprometido; `null` sem limite).
- **Compra** (`POST`/`PUT`): `{descricao (1..160), categoria, tipo: A_VISTA|PARCELADA|ASSINATURA, dataCompra, valor (> 0, até 2 casas), parcelas}`. `valor` é o total da compra (assinatura: o valor mensal); `parcelas` (2..72) só vale em `PARCELADA` e é zerado nos demais. A categoria segue a regra dos gastos (criada se não existir; `Reserva` retorna 422). A saída acrescenta `valorParcela`, `primeiraFatura` (AAAA-MM), `ultimaFatura` (`null` em assinatura) e `parcelasRestantes` (`null` em assinatura).
- **Faturas** (`GET /api/cartoes/{id}/faturas?inicio=AAAA-MM`, padrão mês atual): 12 meses `[{mes, total, itens:[{compraId, descricao, categoria, parcela ("3/10" ou null), valor}]}]`, por mês de **vencimento**.
- **Regra de fechamento/vencimento.** Compra com dia **>=** `diaFechamento` cai na fatura que fecha no mês seguinte; antes, na que fecha no mês da compra. Fechamento/vencimento maior que o último dia do mês vale o último dia. Se `diaVencimento > diaFechamento` a fatura vence no mês do fechamento (fecha 13/11, vence 23/11, com as compras de 13/10 a 12/11); senão, no mês seguinte (fecha 25/11, vence 5/12). A projeção lança cada fatura no mês do vencimento.
- **Parcelas.** `valorTotal / n` com `HALF_UP` e 2 casas; a diferença de arredondamento vai na **primeira** parcela (100,00 em 3x = 33,34 + 33,33 + 33,33).
- **Integração.** Cada compra vira gastos do motor (à vista = pontual; parcelada = parcelado, ou pontual ajustado + parcelado quando há resto; assinatura = recorrente) a partir da primeira fatura. A projeção soma lançamentos + cartões; `GET /api/lancamentos/resumo` inclui o cartão sem `grupo` e em `FIXOS` (a fatura é gasto fixo), nunca em `EVENTUAIS`; com isso a Saúde financeira também considera o cartão. `DELETE /api/categorias/{nome}` retorna 422 se houver compra ativa com a categoria.

## Estrutura

`domain` (cálculo puro, sem Spring) · `application` (serviços) · `infra.persistencia` (JPA) · `api` (REST/DTOs).
