# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versões seguem [SemVer](https://semver.org/lang/pt-BR/) com sufixo `-beta.N` enquanto o projeto estiver em beta.

## [Não lançado]

### Corrigido
- Fatura do cartão: com vencimento depois do fechamento, a fatura vence no mesmo mês em que fecha (fecha 13/11, vence 23/11, com as compras de 13/10 a 12/11), e a projeção lança o cartão no mês em que a fatura é paga (antes ia para o mês seguinte e o mês do pagamento ficava zerado). "Fatura atual" é a fatura aberta, que recebe as compras de hoje, e o limite disponível considera as faturas ainda por pagar.
- Campo de data da aba PLR (e demais campos de data) com tema escuro e texto legível; mais espaço entre os cards de gastos fixos e de cartões de crédito.

### Adicionado
- Saúde financeira: seletor "Mês atual" × "Média de 12 meses". No mês atual, gastos (inclusive a fatura do cartão que vence no mês) e aportes são só os do mês; a média de 12 meses continua como padrão.
- Aba "Cartão de crédito": cadastro de cartões (fechamento, vencimento e limite) e de todas as compras (à vista, parceladas ou assinaturas); a fatura de cada mês entra nos gastos fixos, na projeção, na média por categoria e na Saúde financeira, com limite disponível e faturas dos próximos 12 meses. Em Gastos fixos, um card lista os cartões e permite cadastrar um novo.
- Aba "PLR" com o controle do PLR recebido: registre o que já caiu na conta e os gastos feitos com ele, divida em envelopes (os destinos do plano de uso do PLR, com sugestão 80/20) e veja quanto foi alocado, gasto e quanto sobra em cada um. O controle não altera a projeção. A aba "13º, férias e PLR" passa a se chamar "13º e férias".
- Plano de uso do 13º e do PLR: o usuário divide cada adicional entre lazer, gastos obrigatórios e reservas (opcionalmente ligadas a uma meta); a projeção mostra a divisão no mês em que a parcela cai e a Saúde financeira compara o plano com o 50/30/20.
- FUNCEF como reserva: a contribuição conta como poupança na Saúde financeira; saldo atual da FUNCEF no Perfil e previdência e patrimônio total na projeção.
- Aba "Saúde financeira" com o método 50/30/20 opcional: compara necessidades, desejos e poupança com a renda líquida, classifica cada categoria como obrigatória ou lazer, sugere quanto guardar em Metas e reserva e mostra gráficos.
- Metas e reservas: o aporte mensal sai do saldo disponível de cada mês até a meta atingir o alvo; a projeção mostra o valor guardado em metas e o patrimônio (disponível + guardado).
- Banco onde o dinheiro de cada meta está guardado (texto livre).
- Opção de contar ou não o 13º na projeção, com parcelas fixas em fevereiro e novembro (50% cada).
- Aviso ao tentar ativar o Bônus Caixa com cargo que não é de 8 horas.
- Aba "Gastos mensais": gasto de uma vez só some sozinho depois que o mês passa (exclusão lógica, os dados ficam no banco); exclusões do usuário também são lógicas.
- Campos numéricos com visual próprio, sem os botões nativos do navegador.
- Nome do usuário no perfil (opcional, até 60 caracteres); a tela de Projeção passa a saudar pelo nome.
- PLR opcional na projeção: o usuário escolhe se conta com ele; padrão de 2 parcelas (setembro 50% e março 50%), com mês e valor ajustáveis.
- IR sobre o PLR em tributação exclusiva (tabela própria com vigência): setembro isento e março com os descontos.
- Aviso para não contar com o dinheiro do PLR ao ativá-lo e no detalhe do mês da projeção.

### Alterado
- O plano de uso do 13º/PLR preserva o identificador de cada destino ao ser salvo; remover um destino do PLR que tenha gastos registrados é bloqueado.
- A aba "Gastos mensais" foi dividida em "Gastos fixos" (todo mês e parcelado) e "Gastos eventuais" (uma vez só), cada uma com a sua média por categoria. `/gastos` redireciona para `/gastos-fixos`; `POST /api/lancamentos/novo` aceita `?tipo=` e `GET /api/lancamentos/resumo` aceita `?grupo=FIXOS|EVENTUAIS`.
- Previsão de conclusão das metas passa a ser o mês do último aporte.
- Dropdowns com visual único em todas as telas.

### Corrigido
- Barra da média por categoria volta a aparecer em Gastos mensais.

### Removido
- Parcelas editáveis do 13º e o endpoint `POST /api/decimo/parcelas`.
- Categoria "Reserva" dos gastos (a reserva fica na aba Metas); gastos que a usavam foram para "Outros".

## [0.1.0-beta.1] - 2026-10-20

### Adicionado
- Backend Spring Boot (motor de cálculo de INSS, IRPF, 13º, férias, PLR e bônus; gastos; metas; projeção de 12 meses).
- Frontend React com telas de projeção, perfil, gastos, 13º/férias, metas e parâmetros.
- `docker compose up -d --build` sobe banco, backend e frontend.
- README central com tutorial de subida e troca de senha do banco.
- Fluxo com branches, PRs e CI no GitHub Actions; controle de versão beta.
