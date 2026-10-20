# Gestão Financeira Box — Plano

Aplicação web para projetar os gastos e a renda dos próximos 12 meses, derivada da planilha *Simulador de Gastos - Caixa Econômica Federal*.

## 1. Decisões já tomadas

| Tema | Decisão |
|---|---|
| Backend | Java 21 + Spring Boot 3 (REST), JPA/Hibernate, Flyway |
| Frontend | React + Vite + TypeScript |
| Banco | PostgreSQL |
| Distribuição | Repositório no GitHub; colegas clonam e rodam **localmente**. Hospedagem fica para depois |
| Usuários | Uma instalação = um usuário (sem login na v1). Modelo preparado para adicionar autenticação depois |
| Escopo extra | 13º e férias (1/3), gastos parcelados e pontuais, metas e reserva |

Consequência da distribuição local: `docker compose up` sobe o Postgres, o backend e o frontend. Sem credenciais no repositório, só valores de exemplo em `.env.example`.

## 2. Regras de negócio extraídas da planilha

Todas viram código Java no pacote `domain` (sem dependência de Spring), com testes unitários.

### Receitas
- **Remuneração base:** `MAX(piso do cargo, delta + gratificação)`.
  - Técnico Bancário Novo: piso vem da tabela de **Deltas** (201 a 248, RH 115 Anexo XIII).
  - Demais cargos: piso + gratificação da aba *Valores*.
- **Sobreaviso:** `base × dias × 8 ÷ divisor`. O divisor é 180 para carreiras de 6h e 220 para as de 8h.
- **Auxílio creche (PAI):** `filhos × valor do PAI`.
- **Home office:** valor fixo, se marcado.
- **VR + VA:** valor fixo, opcional.

### Descontos
- **INSS** (sobre base + sobreaviso): `salário × alíquota − parcela a deduzir` pela faixa. Acima de R$ 8.475,55 aplica o teto (R$ 988,09).
- **IRPF:**
  1. Dedução legal = `INSS + FUNCEF`; a aplicável é `MAX(dedução legal, 607,20)`.
  2. Base de cálculo = `MAX(salário − dedução aplicável, 0)`.
  3. Tabela progressiva: até 2.428,80 isento, depois 7,5%, 15%, 22,5% e 27,5%, com parcela a deduzir.
  4. Redução: salário até 5.000 zera o imposto; entre 5.000 e 7.350 reduz `MAX(0, 978,62 − 0,133145 × salário)`; acima disso não há redução.
- **Saúde Caixa:** sozinho `3,5% × base`. Com dependentes `MIN(dep × 502,08 + 3,5% × base, 7% × base)`. Também pode não participar.
- **FUNCEF:** percentual livre × base.
- **Vale-transporte:** 4% do valor de referência.
- **Sindicato:** `MIN(1% × (base + sobreaviso), 149,21)`.

### Resultado
`líquido = receitas + descontos`; `sobra = líquido − gastos`.

## 3. Pontos de atenção

1. **Não existe na planilha e precisa ser especificado:** 13º, férias, parcelados, metas e reserva. Para o 13º e as férias o INSS e o IRPF têm tratamento próprio (o IR do 13º é exclusivo e separado). Preciso confirmar as regras com você antes de codificar.
2. **Tabelas de IR e INSS mudam todo ano.** Ficam no banco com `vigencia_inicio` e `vigencia_fim`, e o cálculo de cada mês usa a tabela vigente naquele mês. Isso importa para uma projeção que pode cruzar a virada do ano.
3. **A aba *Valores* vem de um `IMPORTRANGE`** de outra planilha. Na v1 os valores entram como seed (Flyway) e podem ser editados em uma tela de parâmetros.
4. **Fórmulas a conferir:** o VT usa o valor do delta como referência (parece simplificação); a Saúde Caixa não entra na dedução do IR. Vou reproduzir como está e sinalizar para você validar.
5. **Células soltas** (`I23`, `J23`, `M35:M39` na aba principal) parecem rascunho e serão ignoradas, salvo se você disser o contrário.

## 4. Modelo de dados (rascunho)

- `cargo` (nome, jornada 6h/8h, piso, gratificação, vigência)
- `delta_salarial` (referência, valor, vigência)
- `tabela_inss_faixa` / `tabela_irpf_faixa` (de, até, alíquota, dedução, vigência)
- `parametro_beneficio` (PAI, home office, VR/VA, teto sindicato, valor por dependente do plano, vigência)
- `perfil` (cargo, delta, filhos, dependentes, plano de saúde, % FUNCEF, VT, sindicato, home office, VR/VA)
- `lancamento` (descrição, categoria, valor, tipo: recorrente | pontual | parcelado, mês inicial, mês final ou nº de parcelas)
- `evento_salarial` (reajuste, mudança de delta, 13º, férias, sobreaviso por mês)
- `meta` (nome, valor alvo, prazo, aporte mensal)
- Projeção calculada na hora (não persistida na v1), então sempre reflete o perfil e os parâmetros atuais.

## 5. API (rascunho)

- `GET/PUT /api/perfil`
- `CRUD /api/lancamentos`, `/api/eventos`, `/api/metas`
- `GET /api/projecao?inicio=2026-11` devolve 12 meses com receitas, descontos detalhados, gastos, sobra e saldo acumulado
- `GET/PUT /api/parametros/*` para tabelas e valores

## 6. Telas

1. **Projeção 12 meses:** grade mês a mês, gráfico de saldo acumulado e detalhamento ao clicar no mês (memória de cálculo de INSS e IR).
2. **Gastos:** cadastro de recorrentes, pontuais e parcelados, agrupado por categoria.
3. **Perfil:** cargo, delta, dependentes, plano, FUNCEF e demais opções (equivale ao topo da planilha).
4. **Metas e reserva.**
5. **Parâmetros:** tabelas de INSS, IR, deltas e cargos.

## 7. Estrutura do repositório

```
GestaFinanceiraBox/
  backend/    Spring Boot (domain / application / infra / api)
  frontend/   React + Vite + TS
  docker-compose.yml
  README.md   (como clonar e rodar)
```

## 8. Fases de entrega

1. **Fundação:** estrutura, Docker Compose, Postgres, Flyway com seed das tabelas.
2. **Motor de cálculo** (INSS, IRPF, receitas, descontos) com testes que reproduzem os valores da planilha.
3. **Perfil e projeção de 12 meses** (API + tela principal).
4. **Gastos** recorrentes, pontuais e parcelados.
5. **13º e férias**, após validarmos as regras.
6. **Metas e reserva.**
7. **Telas de parâmetros**, polimento e README para os colegas.

## 9. Decisões complementares

- **13º salário:** definido manualmente pelo usuário. Ele informa os meses e o percentual de cada parcela, e a tela já vem preenchida com o padrão 50% em fevereiro e 50% em novembro. As parcelas ficam em `evento_salarial` (mês, percentual).
  - Premissa a confirmar: INSS e IRPF do 13º incidem sobre o valor integral e são descontados na última parcela do ano civil, e o IR do 13º é calculado separado, em tributação exclusiva.
  - A soma dos percentuais não pode passar de 100%; se ficar abaixo, a tela avisa.
- **Início da projeção:** sempre o mês atual (`YearMonth.now()`), sem parâmetro obrigatório.
- **Exportação Excel/PDF:** fora do escopo inicial.
- **Pacote base:** `br.com.gestafinanceira`.

## 10. Pacotes do motor de cálculo

O motor fica em `br.com.gestafinanceira.domain.calculo`. Ele só usa a biblioteca padrão do Java (`BigDecimal`, `java.time`, `java.util`), sem Spring nem JPA. Assim pode ser testado isoladamente e é fácil de ler e auditar.

```
br.com.gestafinanceira
  domain
    calculo
      remuneracao   RemuneracaoBase, Sobreaviso, Beneficios
      inss          CalculadoraInss, FaixaInss
      irpf          CalculadoraIrpf, FaixaIrpf, ReducaoIrpf
      descontos     SaudeCaixa, Funcef, ValeTransporte, Sindicato
      decimoterceiro Calculadora13, CalculadoraFerias
      projecao      ProjetorMensal (orquestra os 12 meses)
    modelo          Perfil, Lancamento, Meta, EventoSalarial
  application       casos de uso (serviços)
  infra             JPA, repositórios, Flyway
  api               controllers REST e DTOs
```

Regras: valores monetários sempre em `BigDecimal` com arredondamento `HALF_UP` a 2 casas (igual ao `ROUND` do Excel); `domain` não depende de `application`, `infra` nem `api`.

## 11. Perguntas em aberto

- Férias: em qual mês você costuma tirar, e recebe o abono de 1/3 junto com a antecipação do 13º, ou separado? (Posso deixar o mês das férias como um campo configurável no perfil.)
