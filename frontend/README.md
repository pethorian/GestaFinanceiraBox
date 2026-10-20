# Frontend — Gestão Financeira Box

React + Vite + TypeScript. Tema Dracula. Todos os dados e **todos os cálculos** vêm do back end
(Spring Boot + PostgreSQL); o front só exibe e envia alterações.

## Rodar

Requer Node.js 20+ e o back end rodando em `http://localhost:8080` (ver `backend/README.md`).

```bash
npm install
npm run dev        # http://localhost:5173 (o Vite repassa /api para o back end)
npm test           # testes de formatação
npm run build      # typecheck + build de produção
```

Para apontar para outro servidor, defina `VITE_API_URL` (ex.: `https://meu-servidor`).

## Estrutura

```
src/
  domain/      tipos do contrato da API e funções de formatação (sem regras de negócio)
  api/         http.ts (fetch + erros) e api.ts (uma função por endpoint)
  store/       AppContext: carrega tudo da API, grava com pequeno atraso e pede o recálculo ao back end
  components/  Layout, ui (Card, Switch...), gráficos, painel de memória de cálculo
  pages/       Projecao, Perfil, Gastos, Decimo, Metas, Parametros
  styles/      global.css (tema)
```

## De onde vem cada tela

| Tela | Endpoints |
|---|---|
| Projeção e memória de cálculo | `GET /api/projecao` (inclui `resumo`: médias, melhor/pior mês, meses no vermelho) |
| Perfil (prévia do mês) | `GET/PUT /api/perfil`, `GET /api/projecao/mes-comum`, `GET /api/parametros/vigentes` |
| Gastos | `/api/lancamentos`, `GET /api/lancamentos/resumo`, `GET /api/categorias` |
| 13º, férias, PLR e bônus | `GET/PUT /api/decimo`, `GET/PUT /api/ferias`, `GET/PUT /api/plr` (+ `POST /api/plr/parcelas`), `GET/PUT /api/bonus` |
| Metas | `/api/metas` (percentual e previsão calculados no back end) |
| Parâmetros | `GET /api/parametros/vigentes`; "restaurar" usa `POST /api/dados/restaurar` |

Se a API falhar ao salvar, o front mostra o aviso e recarrega do banco o que está realmente salvo.
