# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versões seguem [SemVer](https://semver.org/lang/pt-BR/) com sufixo `-beta.N` enquanto o projeto estiver em beta.

## [Não lançado]

### Adicionado
- Nome do usuário no perfil (opcional, até 60 caracteres); a tela de Projeção passa a saudar pelo nome.
- PLR opcional na projeção: o usuário escolhe se conta com ele; padrão de 2 parcelas (setembro 50% e março 50%), com mês e valor ajustáveis.
- IR sobre o PLR em tributação exclusiva (tabela própria com vigência): setembro isento e março com os descontos.
- Aviso para não contar com o dinheiro do PLR ao ativá-lo e no detalhe do mês da projeção.

## [0.1.0-beta.1] - 2026-10-20

### Adicionado
- Backend Spring Boot (motor de cálculo de INSS, IRPF, 13º, férias, PLR e bônus; gastos; metas; projeção de 12 meses).
- Frontend React com telas de projeção, perfil, gastos, 13º/férias, metas e parâmetros.
- `docker compose up -d --build` sobe banco, backend e frontend.
- README central com tutorial de subida e troca de senha do banco.
- Fluxo com branches, PRs e CI no GitHub Actions; controle de versão beta.
