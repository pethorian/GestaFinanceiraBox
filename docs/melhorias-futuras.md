# Melhorias futuras

Resultado da revisão geral do projeto (outubro de 2026, versão `0.1.0-beta.1`). As correções feitas na própria
revisão estão no `CHANGELOG.md` (seção "Não lançado"); aqui ficam só as sugestões ainda abertas, da mais urgente
para a menos urgente.

## Como os dados se propagam hoje (resumo da revisão)

| Alteração | Projeção | Gastos (resumo por categoria) | Saúde financeira | Metas | PLR / plano de uso |
| --- | --- | --- | --- | --- | --- |
| Gasto fixo / eventual | sim | sim (no grupo certo) | sim | não muda (aporte não depende da sobra) | não muda |
| Compra no cartão | sim (no mês da fatura) | sim (só em Fixos) | sim | não muda | não muda |
| Perfil (cargo, delta etc.) | sim | — | sim (renda) | sim (via 13º/PLR) | sim (corrigido nesta revisão) |
| Meta | sim (aportes) | — | sim (poupança) | sim | sim (nome da meta) |
| Plano de uso 13º/PLR | sim | — | sim (bloco de adicionais) | sim (corrigido nesta revisão) | sim |

Toda a regra de cálculo fica no backend e a tela pede tudo de novo depois de cada gravação (`recalcular` em
`AppContext.tsx`), então os números ficam coerentes entre as abas.

## Prioridade alta

1. **Acesso sem autenticação exposto na rede.** O `docker-compose.yml` publica o backend (`8080`) e o frontend
   (`5173`) em todas as interfaces (`0.0.0.0`), e a API não tem login. Qualquer aparelho na mesma rede (Wi-Fi de
   casa, por exemplo) consegue ler e alterar os dados financeiros. Sugestão: publicar em `127.0.0.1` como o banco já
   faz, ou colocar autenticação simples (Basic Auth no nginx ou Spring Security com um usuário).
2. **Fuso horário do container.** O `Clock` é `systemDefaultZone()`; no Docker o padrão é UTC. Entre 21h e 0h do
   último dia do mês (horário de Brasília), o "mês atual" já vira o mês seguinte: a projeção começa um mês adiante e
   os gastos eventuais do mês são expirados antes da hora. Sugestão: `TZ=America/Sao_Paulo` no serviço `backend`
   (ou `Clock.system(ZoneId.of("America/Sao_Paulo"))`).
3. **Backup automático do banco.** Os dados reais vivem só no volume `dados-pg`. Hoje o backup é manual
   (`docs/persistencia-docker.md`). Sugestão: um serviço/agendamento com `pg_dump` diário guardando os últimos N
   arquivos fora do volume (e fora do git).

## Regras de cálculo a confirmar

Premissas herdadas da planilha, marcadas no código como "a confirmar". Vale validar com um holerite real:

4. **13º salário** (`Calculadora13`): com FUNCEF, o IR da última parcela usa a dedução fixa e não deduz INSS nem
   FUNCEF, o que aumenta o imposto. Pela regra da Receita, o 13º tem tributação exclusiva com dedução do INSS e da
   previdência complementar.
5. **Férias** (`CalculadoraFerias`): o adicional usa só o salário base (sem sobreaviso) e o abono soma `base/3`
   isento. O abono pecuniário de verdade é 1/3 dos dias de férias **mais** o 1/3 sobre ele; vale conferir.
6. **PLR** (`CalculadoraPlr`): todo o IR do ciclo cai na parcela de menor mês (março). Se a antecipação de setembro
   tiver retenção no holerite, a distribuição entre os meses fica diferente (o total do ano não muda).
7. **Bônus Caixa**: entra como valor líquido, sem INSS/IR. Conferir se há retenção.
8. **Previdência FUNCEF**: o saldo projetado soma só a contribuição do participante, sem a contrapartida da
   patrocinadora nem rendimento. Uma taxa de rendimento opcional deixaria o patrimônio total mais realista.

## Funcionalidades

9. **Projeção além de 12 meses** (24/36/60) ou escolha do mês inicial na tela; o backend já aceita `?inicio=`.
10. **Rendimento das metas e do saldo disponível** (ex.: % do CDI ao mês), hoje o dinheiro guardado não rende.
11. **Reajuste salarial previsto** (data-base de setembro): hoje só muda se o usuário cadastrar uma nova vigência
    nos parâmetros.
12. **Receitas extras** (aluguel recebido, freelas, restituição do IR): hoje só existem gastos.
13. **Histórico real × previsto**: registrar o que de fato aconteceu em cada mês e comparar com a projeção (a
    expiração dos gastos eventuais já guarda os dados com exclusão lógica).
14. **Exportar/importar** (CSV/Excel ou JSON) para backup manual e para análise fora do app.
15. **Gastos do cartão por categoria na aba Gastos fixos**: a fatura entra no resumo, mas a lista de gastos fixos
    não mostra as compras; um atalho para a fatura do mês ajudaria.
16. **Alertas**: meses no vermelho já aparecem na Projeção; poderiam também aparecer no menu ou numa notificação.

## Qualidade de código e testes

17. **Validação de valores na API**: `LancamentoDto` não limita o valor (`@Digits`) nem o número de parcelas
    (a tela vai até 60, o cartão até 72). Um valor gigante vira erro 500 do banco em vez de 400.
18. **`LocalDateTime.now()` sem `Clock`** em `PerfilService`, `SaudeFinanceiraService`, `ParametrosService` e nas
    entidades: usar sempre o `Clock` injetado deixa os testes determinísticos (o `PlrService` foi corrigido).
19. **Desempenho do recálculo**: cada edição dispara 11 requisições e a projeção é calculada várias vezes por
    requisição (`/api/dados` calcula a projeção na saúde, no plano de uso e nas metas). Para um usuário é aceitável;
    se crescer, um endpoint único de "recálculo" ou cache por requisição resolve.
20. **Construtores com muitos parâmetros** (`ProjecaoService`, `DadosService`) e `MesProjecao` com 40 campos:
    agrupar em records menores (renda, descontos, adicionais, patrimônio) facilita a manutenção.
21. **Dependência circular Metas ↔ Projeção**: resolvida com `ObjectProvider` no `MetaService`. Se a regra crescer,
    vale extrair um serviço só de "previsão de metas" que dependa da projeção.
22. **Testes de tela**: os testes do frontend renderizam HTML estático; testes de interação (Testing Library) e um
    teste ponta a ponta (Playwright) do fluxo "cadastrar gasto → ver na projeção" dariam mais segurança.
23. **Acessibilidade**: os gráficos não têm alternativa em texto/tabela e algumas cores de status dependem só da
    cor (verde/vermelho).
