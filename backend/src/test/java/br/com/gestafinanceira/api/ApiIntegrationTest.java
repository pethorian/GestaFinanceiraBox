package br.com.gestafinanceira.api;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Sobe a aplicação inteira contra o Postgres de teste (Flyway + seed) e exercita a API. Cada teste faz rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {
    @Autowired MockMvc mvc;

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String body) throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void healthEstaUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void perfilPadraoEAtualizacao() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(status().isOk())
                .andExpect(jsonPath("$.delta", is(228))).andExpect(jsonPath("$.saude", is("DEP")));
        Long cargoId = cargoId(0);
        json(put("/api/perfil"), """
                {"cargoId": %d, "delta": 230, "diasSobreaviso": 2, "filhos": 2, "homeOffice": false, "vrVa": true,
                 "saude": "SOZINHO", "dependentes": 0, "funcefPct": 7.5, "valeTransporte": false, "sindicato": true,
                 "saldoInicial": 1000}""".formatted(cargoId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.delta", is(230)))
                .andExpect(jsonPath("$.funcefPct", is(7.5)));
    }

    @Test
    void perfilInvalidoRetorna400E422() throws Exception {
        json(put("/api/perfil"), """
                {"cargoId": 1, "delta": 228, "diasSobreaviso": 99, "filhos": 0, "homeOffice": false, "vrVa": false,
                 "saude": "NAO", "dependentes": 0, "funcefPct": 0, "valeTransporte": false, "sindicato": false,
                 "saldoInicial": 0}""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalhes", hasSize(1)));
        json(put("/api/perfil"), """
                {"cargoId": 999999, "delta": 228, "diasSobreaviso": 0, "filhos": 0, "homeOffice": false, "vrVa": false,
                 "saude": "NAO", "dependentes": 0, "funcefPct": 0, "valeTransporte": false, "sindicato": false,
                 "saldoInicial": 0}""").andExpect(status().isUnprocessableEntity());
        Long cargoId = cargoId(0);
        json(put("/api/perfil"), """
                {"cargoId": %d, "delta": 999, "diasSobreaviso": 0, "filhos": 0, "homeOffice": false, "vrVa": false,
                 "saude": "NAO", "dependentes": 0, "funcefPct": 0, "valeTransporte": false, "sindicato": false,
                 "saldoInicial": 0}""".formatted(cargoId)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void projecaoDevolveDozeMesesComResumo() throws Exception {
        mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.meses", hasSize(12)))
                .andExpect(jsonPath("$.meses[0].competencia", is("2026-11")))
                .andExpect(jsonPath("$.meses[11].competencia", is("2027-10")))
                .andExpect(jsonPath("$.meses[0].irpf.imposto", notNullValue()))
                .andExpect(jsonPath("$.resumo.saldoFinal", notNullValue()))
                .andExpect(jsonPath("$.meses[0].receitas", greaterThan(0.0)));
        mvc.perform(get("/api/projecao")).andExpect(status().isOk()).andExpect(jsonPath("$.meses", hasSize(12)));
        mvc.perform(get("/api/projecao?inicio=abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/projecao/mes-comum?mes=2026-11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.decimo", is(0.0)));
    }

    @Test
    void lancamentoCrudCompletoERefleteNaProjecao() throws Exception {
        String corpo = """
                {"descricao": "Curso", "categoria": "Pessoal", "tipo": "PARCELADO", "valor": 300,
                 "mesInicial": "2026-11", "parcelas": 4}""";
        String resp = json(post("/api/lancamentos"), corpo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue())).andReturn().getResponse().getContentAsString();
        String id = resp.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(get("/api/lancamentos/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas", is(4)));
        mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(status().isOk());

        json(put("/api/lancamentos/" + id), """
                {"descricao": "Curso", "categoria": "Pessoal", "tipo": "PONTUAL", "valor": 500,
                 "mesInicial": "2027-01", "parcelas": 9}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo", is("PONTUAL"))).andExpect(jsonPath("$.parcelas", is(0)));

        mvc.perform(delete("/api/lancamentos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/lancamentos/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/lancamentos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void lancamentoParceladoSemParcelasUsaMinimoDoServidor() throws Exception {
        json(post("/api/lancamentos"), """
                {"descricao": "x", "categoria": "Outros", "tipo": "PARCELADO", "valor": 10,
                 "mesInicial": "2026-11", "parcelas": 0}""").andExpect(status().isCreated())
                .andExpect(jsonPath("$.parcelas", is(2)));
        json(post("/api/lancamentos"), """
                {"descricao": "", "categoria": "Outros", "tipo": "PONTUAL", "valor": -1,
                 "mesInicial": "2026-11", "parcelas": 0}""").andExpect(status().isBadRequest());
    }

    @Test
    void decimoValidaSomaEMesesRepetidos() throws Exception {
        mvc.perform(get("/api/decimo")).andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas", hasSize(2))).andExpect(jsonPath("$.totalPercentual", is(100.0)));
        json(put("/api/decimo"), """
                {"parcelas": [{"mes": 2, "percentual": 60}, {"mes": 11, "percentual": 60}]}""")
                .andExpect(status().isUnprocessableEntity());
        json(put("/api/decimo"), """
                {"parcelas": [{"mes": 2, "percentual": 30}, {"mes": 2, "percentual": 30}]}""")
                .andExpect(status().isUnprocessableEntity());
        json(put("/api/decimo"), """
                {"parcelas": [{"mes": 12, "percentual": 40}]}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.aviso", notNullValue()));
    }

    @Test
    void feriasPodemSerAlteradas() throws Exception {
        json(put("/api/ferias"), """
                {"ativo": true, "mes": 1, "abono": true}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.mes", is(1))).andExpect(jsonPath("$.abono", is(true)));
        json(put("/api/ferias"), """
                {"ativo": true, "mes": 13, "abono": true}""").andExpect(status().isBadRequest());
    }

    @Test
    void metasCrudComPrevisao() throws Exception {
        mvc.perform(get("/api/metas")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        String resp = json(post("/api/metas"), """
                {"nome": "Viagem", "valorAlvo": 10000, "valorAtual": 1000, "aporteMensal": 1500}""")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.falta", is(9000)))
                .andExpect(jsonPath("$.mesesParaAtingir", is(6))).andReturn().getResponse().getContentAsString();
        String id = resp.replaceAll(".*\"id\":(\\d+).*", "$1");
        json(put("/api/metas/" + id), """
                {"nome": "Viagem", "valorAlvo": 10000, "valorAtual": 1000, "aporteMensal": 0}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.mesesParaAtingir").doesNotExist());
        mvc.perform(delete("/api/metas/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/metas/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void categoriasListamECrudBasico() throws Exception {
        mvc.perform(get("/api/categorias")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(10)));
        json(post("/api/categorias"), "{\"nome\": \"Educação\", \"ordem\": 0}").andExpect(status().isCreated());
        json(post("/api/categorias"), "{\"nome\": \"Educação\", \"ordem\": 0}").andExpect(status().isUnprocessableEntity());
        mvc.perform(delete("/api/categorias/Educação")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/categorias/Moradia")).andExpect(status().isUnprocessableEntity()); // em uso
    }

    @Test
    void parametrosListagens() throws Exception {
        mvc.perform(get("/api/parametros/cargos")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(13)));
        mvc.perform(get("/api/parametros/inss")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].faixas", hasSize(4)));
        mvc.perform(get("/api/parametros/irpf")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].faixas", hasSize(5)));
        mvc.perform(get("/api/parametros/deltas")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deltas", hasSize(48)));
        mvc.perform(get("/api/parametros/beneficios")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pai", is(729.21)))
                .andExpect(jsonPath("$[0].homeOffice", is(104.48)))
                .andExpect(jsonPath("$[0].vrVa", is(2194.16)))
                .andExpect(jsonPath("$[0].bonusCaixaMaxSalarios", is(3.0)))
                .andExpect(jsonPath("$[0].vtReferencia").doesNotExist());
    }

    @Test
    void novaVigenciaDeInssAfetaApenasMesesPosteriores() throws Exception {
        String base = mvc.perform(get("/api/projecao?inicio=2026-11")).andReturn().getResponse().getContentAsString();
        json(put("/api/parametros/inss"), """
                {"vigenciaInicio": "2027-01-01", "faixas": [
                  {"ate": 2000, "aliquota": 0.10, "deducao": 0},
                  {"ate": 9000, "aliquota": 0.20, "deducao": 100}]}""").andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].vigenciaFim", is("2026-12-31")))
                .andExpect(jsonPath("$[1].vigenciaFim").doesNotExist());
        String depois = mvc.perform(get("/api/projecao?inicio=2026-11")).andReturn().getResponse().getContentAsString();
        // nov/2026 e dez/2026 não mudam; jan/2027 em diante sim
        org.assertj.core.api.Assertions.assertThat(mesInss(depois, 0)).isEqualTo(mesInss(base, 0));
        org.assertj.core.api.Assertions.assertThat(mesInss(depois, 2)).isNotEqualTo(mesInss(base, 2));

        mvc.perform(delete("/api/parametros/inss/2027-01-01")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(delete("/api/parametros/inss/2026-01-01")).andExpect(status().isUnprocessableEntity());
    }

    private static String mesInss(String json, int indice) {
        String[] partes = json.split("\"indice\":");
        String trecho = partes[indice + 1];
        return trecho.replaceAll("(?s).*?\"inss\":([0-9.]+).*", "$1");
    }

    @Test
    void faixasInvalidasSaoRejeitadas() throws Exception {
        json(put("/api/parametros/inss"), """
                {"vigenciaInicio": "2027-01-01", "faixas": [{"ate": null, "aliquota": 0.1, "deducao": 0}]}""")
                .andExpect(status().isUnprocessableEntity());
        json(put("/api/parametros/irpf"), """
                {"vigenciaInicio": "2027-01-01", "faixas": [{"ate": null, "aliquota": 0.1, "deducao": 0},
                 {"ate": null, "aliquota": 0.2, "deducao": 0}]}""").andExpect(status().isUnprocessableEntity());
        json(put("/api/parametros/inss"), """
                {"vigenciaInicio": "2027-01-01", "faixas": []}""").andExpect(status().isBadRequest());
    }

    @Test
    void cargoCrudEProtecaoDeCargoEmUso() throws Exception {
        String resp = json(post("/api/parametros/cargos"), """
                {"nome": "Cargo Teste", "jornada": 8, "usaDelta": false, "ordem": 99,
                 "piso": 5000, "gratificacao": 1000, "vigenciaInicio": "2026-01-01"}""")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = resp.replaceAll(".*\"id\":(\\d+).*", "$1");
        json(post("/api/parametros/cargos"), """
                {"nome": "cargo teste", "jornada": 8, "usaDelta": false, "ordem": 99, "piso": 1, "gratificacao": 1}""")
                .andExpect(status().isUnprocessableEntity());
        json(put("/api/parametros/cargos/" + id), """
                {"nome": "Cargo Teste", "jornada": 8, "usaDelta": false, "ordem": 99,
                 "piso": 5500, "gratificacao": 1000, "vigenciaInicio": "2027-01-01"}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.piso", is(5000)));
        mvc.perform(get("/api/parametros/cargos?data=2027-02-01")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nome=='Cargo Teste')].piso").value(5500));
        mvc.perform(delete("/api/parametros/cargos/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/parametros/cargos/" + cargoId(0))).andExpect(status().isUnprocessableEntity()); // em uso
    }

    @Test
    void dadosAgregadosERestaurarPadrao() throws Exception {
        mvc.perform(delete("/api/lancamentos/" + primeiroLancamentoId())).andExpect(status().isNoContent());
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lancamentos", hasSize(7)))
                .andExpect(jsonPath("$.metas", hasSize(3)))
                .andExpect(jsonPath("$.decimo.parcelas", hasSize(2)))
                .andExpect(jsonPath("$.plr.ativo", is(false)))
                .andExpect(jsonPath("$.plr.parcelas", hasSize(2)))
                .andExpect(jsonPath("$.bonus.ativo", is(false)));
        mvc.perform(get("/api/dados")).andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil.cargoId", notNullValue()))
                .andExpect(jsonPath("$.ferias.mes", is(7)));
    }

    @Test
    void beneficiosNovaVigencia() throws Exception {
        String corpo = """
                {"vigenciaInicio": "2027-01-01", "pai": 700, "homeOffice": 1100, "vrVa": 1500,
                 "vtPercentual": 0.04, "sindicatoPercentual": 0.01, "sindicatoTeto": 150, "saudePercentual": 0.035,
                 "saudeTetoPercentual": 0.07, "planoPorDependente": 510, "inssTeto": 1000, "inssTetoSalario": 8600,
                 "irDeducaoMinima": 607.2, "irLimiteIsencao": 5000, "irLimiteReducao": 7350, "irReducaoA": 978.62,
                 "irReducaoB": 0.133145, "divisorJornada6h": 180, "divisorJornada8h": 220,
                 "bonusCaixaMaxSalarios": 4}""";
        json(put("/api/parametros/beneficios"), corpo).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(delete("/api/parametros/beneficios/2027-01-01")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void novasLinhasNascemComPadroesDoServidor() throws Exception {
        mvc.perform(post("/api/lancamentos/novo")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao", is("Novo gasto"))).andExpect(jsonPath("$.tipo", is("RECORRENTE")));
        mvc.perform(post("/api/metas/novo")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome", is("Nova meta"))).andExpect(jsonPath("$.mesesParaAtingir", is(20)));
        // 13º já está 100% distribuído: o servidor recusa nova parcela
        mvc.perform(post("/api/decimo/parcelas")).andExpect(status().isUnprocessableEntity());
        json(put("/api/decimo"), """
                {"parcelas": [{"mes": 12, "percentual": 40}]}""").andExpect(status().isOk());
        mvc.perform(post("/api/decimo/parcelas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas", hasSize(2)))
                .andExpect(jsonPath("$.parcelas[0].mes", is(11)))
                .andExpect(jsonPath("$.parcelas[0].percentual", is(60)))
                .andExpect(jsonPath("$.totalPercentual", is(100)));
    }

    @Test
    void parametrosVigentesEResumos() throws Exception {
        mvc.perform(get("/api/parametros/vigentes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.cargos", hasSize(13)))
                .andExpect(jsonPath("$.deltas.deltas", hasSize(48)))
                .andExpect(jsonPath("$.inss.faixas", hasSize(4)))
                .andExpect(jsonPath("$.irpf.faixas", hasSize(5)))
                .andExpect(jsonPath("$.beneficios.pai", is(729.21)))
                .andExpect(jsonPath("$.deltas.deltas[0].valor", is(4351.71)))
                .andExpect(jsonPath("$.deltas.deltas[47].valor", is(12704.78)))
                .andExpect(jsonPath("$.cargos[0].plrAnual", is(13055.13)))
                .andExpect(jsonPath("$.cargos[11].plrAnual", is(33297.67)))
                .andExpect(jsonPath("$.cargos[12].gratificacao", is(25027.83)))
                .andExpect(jsonPath("$.cargos[?(@.jornada==6)]", hasSize(6)))
                .andExpect(jsonPath("$.cargos[?(@.jornada==8)]", hasSize(7)))
                .andExpect(jsonPath("$.cargos[?(@.usaDelta==false)]", hasSize(0)));
        mvc.perform(get("/api/lancamentos/resumo?inicio=2026-11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias[0].categoria", notNullValue()))
                .andExpect(jsonPath("$.mediaMensalTotal", greaterThan(0.0)));
        mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.melhorMes.competencia", notNullValue()))
                .andExpect(jsonPath("$.resumo.piorMes.competencia", notNullValue()))
                .andExpect(jsonPath("$.resumo.sobraMedia", notNullValue()))
                .andExpect(jsonPath("$.resumo.variacaoSaldo", notNullValue()))
                .andExpect(jsonPath("$.meses[0].irpf.deducaoMinima", is(607.2)))
                .andExpect(jsonPath("$.meses[0].tetoInssAplicado", is(false)));
        mvc.perform(get("/api/metas")).andExpect(jsonPath("$[0].percentual", notNullValue()));
    }

    // ---- FUNCEF ---------------------------------------------------------------------------------

    private String perfilJson(Long cargoId, String funcef) {
        return """
                {"cargoId": %d, "delta": 228, "diasSobreaviso": 0, "filhos": 0, "homeOffice": false, "vrVa": false,
                 "saude": "NAO", "dependentes": 0, "funcefPct": %s, "valeTransporte": false, "sindicato": false,
                 "saldoInicial": 0}""".formatted(cargoId, funcef);
    }

    @Test
    void funcefSoAceitaZeroOuEntreCincoEDoze() throws Exception {
        Long cargo = cargoId(0);
        for (String invalido : new String[] {"3", "13", "4.99", "12.01", "100"}) {
            json(put("/api/perfil"), perfilJson(cargo, invalido)).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("FUNCEF")));
        }
        for (String valido : new String[] {"0", "5", "12", "7.5"}) {
            json(put("/api/perfil"), perfilJson(cargo, valido)).andExpect(status().isOk());
        }
    }

    // ---- PLR --------------------------------------------------------------------------------------

    @Test
    void plrPadraoInativoComParcelasSetMarEValidacoes() throws Exception {
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk());
        mvc.perform(get("/api/plr")).andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", is(false)))
                .andExpect(jsonPath("$.parcelas", hasSize(2))).andExpect(jsonPath("$.totalPercentual", is(100)))
                .andExpect(jsonPath("$.parcelas[0].mes", is(3)))
                .andExpect(jsonPath("$.parcelas[1].mes", is(9)))
                .andExpect(jsonPath("$.parcelas[1].valor").value(org.hamcrest.Matchers.nullValue()))
                // 13055,13 × 50% = 6527,57 cada; saldo (mar) carrega o IR do total (598,02); set isento
                .andExpect(jsonPath("$.parcelas[0].valorBruto", is(6527.57)))
                .andExpect(jsonPath("$.parcelas[0].desconto", is(598.02)))
                .andExpect(jsonPath("$.parcelas[0].valorLiquido", is(5929.55)))
                .andExpect(jsonPath("$.parcelas[1].desconto", is(0.0)))
                .andExpect(jsonPath("$.parcelas[1].valorLiquido", is(6527.57)))
                .andExpect(jsonPath("$.aviso").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.estimativaAnual", is(13055.13))); // Técnico Bancário Novo
        json(put("/api/plr"), """
                {"ativo": true, "parcelas": [{"mes": 3, "percentual": 60}, {"mes": 9, "percentual": 60}]}""")
                .andExpect(status().isUnprocessableEntity());
        json(put("/api/plr"), """
                {"parcelas": [{"mes": 3, "percentual": 30}, {"mes": 3, "percentual": 30}]}""")
                .andExpect(status().isUnprocessableEntity());
        json(put("/api/plr"), """
                {"parcelas": [{"mes": 3, "percentual": 0}]}""").andExpect(status().isBadRequest());
        json(put("/api/plr"), """
                {"parcelas": [{"mes": 3, "percentual": 50, "valor": -1}]}""").andExpect(status().isBadRequest());
        json(put("/api/plr"), """
                {"ativo": true, "parcelas": [{"mes": 3, "percentual": 40}]}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", is(true)))
                .andExpect(jsonPath("$.aviso", notNullValue()));
        // 13º intacto: gravar PLR não apaga as parcelas do 13º
        mvc.perform(get("/api/decimo")).andExpect(jsonPath("$.parcelas", hasSize(2)));
        // ativar sem informar parcelas (lista vazia) volta às padrão
        json(put("/api/plr"), """
                {"ativo": true, "parcelas": []}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas", hasSize(2)))
                .andExpect(jsonPath("$.parcelas[0].mes", is(3))).andExpect(jsonPath("$.parcelas[1].mes", is(9)));
        // ativo omitido mantém o estado atual (retrocompatível)
        json(put("/api/plr"), """
                {"parcelas": [{"mes": 3, "percentual": 50}, {"mes": 9, "percentual": 50}]}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", is(true)));
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk());
    }

    @Test
    void plrInativoNaoEntraEAtivoMostraSetIsentoMarcoComIrValorEMesEditaveis() throws Exception {
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk());
        String semPlr = mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.meses[*].plr", everyItem(is(0.0))))
                .andReturn().getResponse().getContentAsString();
        // ativa com as padrão; início 2026-11 → março = índice 4, setembro = índice 10
        json(put("/api/plr"), """
                {"ativo": true}""").andExpect(status().isOk()).andExpect(jsonPath("$.ativo", is(true)));
        mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.meses[4].plr", is(5929.55)))
                .andExpect(jsonPath("$.meses[4].plrBruto", is(6527.57)))
                .andExpect(jsonPath("$.meses[4].plrDesconto", is(598.02)))
                .andExpect(jsonPath("$.meses[4].evento", is("PLR")))
                .andExpect(jsonPath("$.meses[10].plr", is(6527.57)))
                .andExpect(jsonPath("$.meses[10].plrDesconto", is(0.0)))
                .andExpect(jsonPath("$.meses[10].evento", is("PLR")))
                .andExpect(jsonPath("$.meses[0].plr", is(0.0)));
        String depois = mvc.perform(get("/api/projecao?inicio=2026-11")).andReturn().getResponse().getContentAsString();
        for (int i = 0; i < 12; i++) {
            org.assertj.core.api.Assertions.assertThat(mesInss(depois, i)).isEqualTo(mesInss(semPlr, i));
        }
        // valor informado substitui o calculado; mês alterado (set → ago); 10.000 + 6.527,57 = 16.527,57 → IR 1.378,28
        json(put("/api/plr"), """
                {"ativo": true, "parcelas": [{"mes": 8, "percentual": 50, "valor": 10000}, {"mes": 3, "percentual": 50}]}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas[1].valor", is(10000)))
                .andExpect(jsonPath("$.parcelas[1].valorBruto", is(10000.0)));
        mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.meses[4].plrDesconto", is(1378.28)))
                .andExpect(jsonPath("$.meses[4].plr", is(5149.29)))
                .andExpect(jsonPath("$.meses[9].plr", is(10000.0)))
                .andExpect(jsonPath("$.meses[9].evento", is("PLR")))
                .andExpect(jsonPath("$.meses[10].plr", is(0.0)));
        // desativar zera de novo, mantendo as parcelas gravadas
        json(put("/api/plr"), """
                {"ativo": false}""").andExpect(status().isOk()).andExpect(jsonPath("$.parcelas", hasSize(2)));
        mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(jsonPath("$.meses[*].plr", everyItem(is(0.0))));
        mvc.perform(post("/api/dados/restaurar")).andExpect(jsonPath("$.plr.ativo", is(false)))
                .andExpect(jsonPath("$.plr.parcelas", hasSize(2)));
    }

    // ---- bônus Caixa ------------------------------------------------------------------------------

    private Long cargoIdPorNome(String nome) throws Exception {
        String r = mvc.perform(get("/api/parametros/cargos")).andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\":(\\d+),\"nome\":\"" + nome + "\"").matcher(r);
        if (!m.find()) throw new IllegalStateException("cargo não encontrado: " + nome);
        return Long.valueOf(m.group(1));
    }

    @Test
    void bonusPadraoInativoERejeitadoParaCargoDeSeisHoras() throws Exception {
        mvc.perform(get("/api/bonus")).andExpect(status().isOk()).andExpect(jsonPath("$.ativo", is(false)))
                .andExpect(jsonPath("$.mes", is(12))).andExpect(jsonPath("$.salarios", is(0.0)));
        // perfil padrão = Técnico Bancário Novo (6h)
        json(put("/api/bonus"), """
                {"ativo": true, "mes": 12, "salarios": 2}""").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("8h")));
        json(put("/api/bonus"), """
                {"ativo": false, "mes": 6, "salarios": 1}""").andExpect(status().isOk());
    }

    @Test
    void bonusEmCargoDeOitoHorasValidaMaximoEEntraEmReceitas() throws Exception {
        Long supervisor = cargoIdPorNome("Supervisor de TI");
        json(put("/api/perfil"), perfilJson(supervisor, "6")).andExpect(status().isOk());
        json(put("/api/bonus"), """
                {"ativo": true, "mes": 12, "salarios": 3.5}""").andExpect(status().isUnprocessableEntity());
        json(put("/api/bonus"), """
                {"ativo": true, "mes": 13, "salarios": 1}""").andExpect(status().isBadRequest());
        json(put("/api/bonus"), """
                {"ativo": true, "mes": 12, "salarios": -1}""").andExpect(status().isBadRequest());
        String antes = mvc.perform(get("/api/projecao?inicio=2026-11")).andReturn().getResponse().getContentAsString();
        json(put("/api/bonus"), """
                {"ativo": true, "mes": 12, "salarios": 2.5}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", is(true))).andExpect(jsonPath("$.salarios", is(2.5)));
        // base do Supervisor com delta 228: MAX(18553,08, 8035,13 + 8047,68) = 18553,08 → bônus 46382,70
        mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.meses[1].competencia", is("2026-12")))
                .andExpect(jsonPath("$.meses[1].bonus", is(46382.7)))
                .andExpect(jsonPath("$.meses[1].evento", is("Bônus")))
                .andExpect(jsonPath("$.meses[0].bonus", is(0.0)));
        String depois = mvc.perform(get("/api/projecao?inicio=2026-11")).andReturn().getResponse().getContentAsString();
        for (int i = 0; i < 12; i++) {
            org.assertj.core.api.Assertions.assertThat(mesInss(depois, i)).isEqualTo(mesInss(antes, i));
        }
        mvc.perform(get("/api/dados")).andExpect(jsonPath("$.bonus.ativo", is(true)));
        // com o bônus ativo, não dá para trocar para cargo de 6h
        json(put("/api/perfil"), perfilJson(cargoId(0), "6")).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/dados/restaurar")).andExpect(jsonPath("$.bonus.ativo", is(false)));
    }

    @Test
    void valeTransporteUsaOValorDoDeltaDoPerfil() throws Exception {
        // perfil padrão: delta 228 (8035,13) → 4% = 321,41
        mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(jsonPath("$.meses[0].valeTransporte", is(321.41)));
        json(put("/api/perfil"), """
                {"cargoId": %d, "delta": 201, "diasSobreaviso": 0, "filhos": 0, "homeOffice": false, "vrVa": false,
                 "saude": "NAO", "dependentes": 0, "funcefPct": 0, "valeTransporte": true, "sindicato": false,
                 "saldoInicial": 0}""".formatted(cargoId(0))).andExpect(status().isOk());
        mvc.perform(get("/api/projecao?inicio=2026-11")).andExpect(jsonPath("$.meses[0].valeTransporte", is(174.07)));
    }

    @Test
    void cargoAceitaEGravaPlrAnual() throws Exception {
        String resp = json(post("/api/parametros/cargos"), """
                {"nome": "Cargo PLR", "jornada": 8, "usaDelta": true, "ordem": 99,
                 "piso": 5000, "gratificacao": 1000, "plrAnual": 12345.67, "vigenciaInicio": "2026-01-01"}""")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.plrAnual", is(12345.67)))
                .andReturn().getResponse().getContentAsString();
        String id = resp.replaceAll(".*\"id\":(\\d+).*", "$1");
        json(put("/api/parametros/cargos/" + id), """
                {"nome": "Cargo PLR", "jornada": 8, "usaDelta": true, "ordem": 99,
                 "piso": 5000, "gratificacao": 1000, "plrAnual": 20000, "vigenciaInicio": "2026-01-01"}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.plrAnual", is(20000)));
    }

    @Test
    void nomeDoPerfilPadraoENuloEAparadoAoGravar() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(status().isOk()).andExpect(jsonPath("$.nome").doesNotExist());
        json(put("/api/perfil"), perfilComNome(cargoId(0), ", \"nome\": \"  Ana Maria  \""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome", is("Ana Maria")));
        mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.nome", is("Ana Maria")));
        mvc.perform(get("/api/dados")).andExpect(jsonPath("$.perfil.nome", is("Ana Maria")));
    }

    @Test
    void nomeVazioOuEmBrancoLimpaParaNulo() throws Exception {
        json(put("/api/perfil"), perfilComNome(cargoId(0), ", \"nome\": \"Ana\"")).andExpect(status().isOk());
        json(put("/api/perfil"), perfilComNome(cargoId(0), ", \"nome\": \"   \""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value(org.hamcrest.Matchers.nullValue()));
        json(put("/api/perfil"), perfilComNome(cargoId(0), ", \"nome\": \"Ana\"")).andExpect(status().isOk());
        json(put("/api/perfil"), perfilComNome(cargoId(0), ", \"nome\": \"\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void nomeComMaisDe60CaracteresRetorna400() throws Exception {
        json(put("/api/perfil"), perfilComNome(cargoId(0), ", \"nome\": \"" + "a".repeat(61) + "\""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalhes", hasSize(1)));
        json(put("/api/perfil"), perfilComNome(cargoId(0), ", \"nome\": \"" + "a".repeat(60) + "\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome", is("a".repeat(60))));
    }

    @Test
    void putSemNomeMantemONomeAtual() throws Exception {
        json(put("/api/perfil"), perfilComNome(cargoId(0), ", \"nome\": \"Ana\"")).andExpect(status().isOk());
        json(put("/api/perfil"), perfilComNome(cargoId(0), "")).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", is("Ana")));
        mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.nome", is("Ana")));
    }

    @Test
    void restaurarPadraoMantemONome() throws Exception {
        json(put("/api/perfil"), perfilComNome(cargoId(0), ", \"nome\": \"Ana\"")).andExpect(status().isOk());
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil.nome", is("Ana")))
                .andExpect(jsonPath("$.perfil.delta", is(228)));
    }

    /** Perfil válido (padrão) com o trecho extra do campo nome, já com vírgula inicial quando houver. */
    private String perfilComNome(Long cargoId, String trechoNome) {
        return """
                {"cargoId": %d, "delta": 228, "diasSobreaviso": 0, "filhos": 1, "homeOffice": true, "vrVa": false,
                 "saude": "DEP", "dependentes": 2, "funcefPct": 6, "valeTransporte": true, "sindicato": true,
                 "saldoInicial": 3200%s}""".formatted(cargoId, trechoNome);
    }

    private Long cargoId(int posicao) throws Exception {
        String r = mvc.perform(get("/api/parametros/cargos")).andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(r);
        for (int i = 0; i <= posicao; i++) m.find();
        return Long.valueOf(m.group(1));
    }

    private Long primeiroLancamentoId() throws Exception {
        String r = mvc.perform(get("/api/lancamentos")).andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(r);
        m.find();
        return Long.valueOf(m.group(1));
    }
}
