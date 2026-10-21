package br.com.gestafinanceira.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Plano de uso do 13º e do PLR (Clock fixo em 15/10/2026: 13º em nov/26 e fev/27). Cada teste faz rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdicionaisIntegrationTest {
    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock clockFixo() {
            return Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private ResultActions json(MockHttpServletRequestBuilder req, String body) throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode ler(ResultActions r) throws Exception {
        return om.readTree(r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private ResultActions putDestinos(String evento, String corpo) throws Exception {
        return json(put("/api/adicionais/" + evento + "/destinos"), corpo);
    }

    private JsonNode projecao() throws Exception {
        return ler(mvc.perform(get("/api/projecao?inicio=2026-10")).andExpect(status().isOk()));
    }

    private String criarMeta(String nome) throws Exception {
        return ler(json(post("/api/metas"), """
                {"nome": "%s", "valorAlvo": 100000, "valorAtual": 0, "aporteMensal": 0}""".formatted(nome))
                .andExpect(status().isCreated())).get("id").asText();
    }

    private static BigDecimal v(JsonNode n) {
        return n.decimalValue();
    }

    /** Estado conhecido: 13º ativo, PLR inativo com as parcelas padrão (set e mar), sem destinos. */
    @BeforeEach
    void preparar() throws Exception {
        json(put("/api/decimo"), "{\"ativo\": true}").andExpect(status().isOk());
        json(put("/api/plr"), """
                {"ativo": false, "parcelas": [{"mes": 9, "percentual": 50}, {"mes": 3, "percentual": 50}]}""")
                .andExpect(status().isOk());
        putDestinos("decimo", "[]").andExpect(status().isOk());
        putDestinos("plr", "[]").andExpect(status().isOk());
    }

    // ---- GET / PUT -----------------------------------------------------------------------------

    @Test
    void padraoSemDestinosComPrevia() throws Exception {
        JsonNode r = ler(mvc.perform(get("/api/adicionais/decimo/destinos")).andExpect(status().isOk()));
        assertThat(r.get("evento").asText()).isEqualTo("DECIMO");
        assertThat(r.get("ativo").asBoolean()).isTrue();
        assertThat(r.get("destinos")).isEmpty();
        assertThat(v(r.get("totalPercentual"))).isEqualByComparingTo("0");
        assertThat(v(r.get("semDestinoPercentual"))).isEqualByComparingTo("100");
        JsonNode previa = r.get("previa");
        assertThat(previa).hasSize(2);
        assertThat(previa.get(0).get("mes").asText()).isEqualTo("2026-11");
        assertThat(previa.get(1).get("mes").asText()).isEqualTo("2027-02");
        assertThat(v(previa.get(0).get("semDestino"))).isEqualByComparingTo(v(previa.get(0).get("liquido")));
        assertThat(v(previa.get(0).get("liquido"))).isPositive();
    }

    @Test
    void putSubstituiAListaApararNomeECalculaPrevia() throws Exception {
        String metaId = criarMeta("Viagem");
        JsonNode r = ler(putDestinos("decimo", """
                [{"nome": "  Lazer do fim de ano  ", "tipo": "LAZER", "percentual": 60},
                 {"nome": "Viagem", "tipo": "RESERVA", "percentual": 30, "metaId": %s}]""".formatted(metaId))
                .andExpect(status().isOk()));
        assertThat(r.get("destinos")).hasSize(2);
        assertThat(r.get("destinos").get(0).get("nome").asText()).isEqualTo("Lazer do fim de ano");
        assertThat(r.get("destinos").get(0).get("metaId").isNull()).isTrue();
        assertThat(r.get("destinos").get(1).get("metaId").asText()).isEqualTo(metaId);
        assertThat(r.get("destinos").get(1).get("metaNome").asText()).isEqualTo("Viagem");
        assertThat(r.get("destinos").get(0).get("id").isNumber()).isTrue();
        assertThat(v(r.get("totalPercentual"))).isEqualByComparingTo("90");
        assertThat(v(r.get("semDestinoPercentual"))).isEqualByComparingTo("10");
        JsonNode nov = r.get("previa").get(0);
        BigDecimal liq = v(nov.get("liquido"));
        BigDecimal lazer = v(nov.get("destinos").get(0).get("valor"));
        BigDecimal viagem = v(nov.get("destinos").get(1).get("valor"));
        assertThat(lazer).isEqualByComparingTo(liq.multiply(new BigDecimal("0.6")).setScale(2, java.math.RoundingMode.HALF_UP));
        assertThat(lazer.add(viagem).add(v(nov.get("semDestino")))).isEqualByComparingTo(liq);
        assertThat(nov.get("destinos").get(0).get("tipo").asText()).isEqualTo("LAZER");

        // GET devolve o mesmo; outro PUT substitui tudo
        mvc.perform(get("/api/adicionais/decimo/destinos")).andExpect(jsonPath("$.destinos", hasSize(2)));
        putDestinos("decimo", "[{\"nome\": \"IPVA\", \"tipo\": \"OBRIGATORIO\", \"percentual\": 100}]")
                .andExpect(status().isOk()).andExpect(jsonPath("$.destinos", hasSize(1)))
                .andExpect(jsonPath("$.destinos[0].nome", is("IPVA")))
                .andExpect(jsonPath("$.semDestinoPercentual").value(0));
        // eventos são independentes
        mvc.perform(get("/api/adicionais/plr/destinos")).andExpect(jsonPath("$.destinos", hasSize(0)));
    }

    @Test
    void validacoes() throws Exception {
        putDestinos("decimo", "[{\"nome\": \"   \", \"tipo\": \"LAZER\", \"percentual\": 10}]")
                .andExpect(status().isBadRequest());
        putDestinos("decimo", "[{\"nome\": \"" + "N".repeat(61) + "\", \"tipo\": \"LAZER\", \"percentual\": 10}]")
                .andExpect(status().isBadRequest());
        putDestinos("decimo", "[{\"nome\": \"  " + "N".repeat(60) + "  \", \"tipo\": \"LAZER\", \"percentual\": 10}]")
                .andExpect(status().isOk());
        putDestinos("decimo", "[{\"nome\": \"A\", \"tipo\": \"LAZER\", \"percentual\": 0}]")
                .andExpect(status().isBadRequest());
        putDestinos("decimo", "[{\"nome\": \"A\", \"tipo\": \"LAZER\", \"percentual\": 100.5}]")
                .andExpect(status().isBadRequest());
        putDestinos("decimo", "[{\"nome\": \"A\", \"tipo\": \"LAZER\", \"percentual\": 10.555}]")
                .andExpect(status().isBadRequest());
        putDestinos("decimo", "[{\"nome\": \"A\", \"tipo\": \"LAZER\"}]").andExpect(status().isBadRequest());
        putDestinos("decimo", "[{\"nome\": \"A\", \"percentual\": 10}]").andExpect(status().isBadRequest());
        putDestinos("decimo", "[{\"nome\": \"A\", \"tipo\": \"VIAGEM\", \"percentual\": 10}]")
                .andExpect(status().isBadRequest());
        putDestinos("decimo", "{\"nome\": \"A\"}").andExpect(status().isBadRequest());
        // regra de negócio: 422
        putDestinos("decimo", """
                [{"nome": "A", "tipo": "LAZER", "percentual": 60}, {"nome": "B", "tipo": "RESERVA", "percentual": 50}]""")
                .andExpect(status().isUnprocessableEntity());
        String metaId = criarMeta("Viagem");
        putDestinos("decimo", "[{\"nome\": \"A\", \"tipo\": \"LAZER\", \"percentual\": 10, \"metaId\": " + metaId + "}]")
                .andExpect(status().isUnprocessableEntity());
        putDestinos("decimo", "[{\"nome\": \"A\", \"tipo\": \"RESERVA\", \"percentual\": 10, \"metaId\": 999999}]")
                .andExpect(status().isUnprocessableEntity());
        // evento inexistente: 404
        mvc.perform(get("/api/adicionais/ferias/destinos")).andExpect(status().isNotFound());
        putDestinos("bonus", "[]").andExpect(status().isNotFound());
        // nada do que falhou foi gravado (só o nome de 60 caracteres)
        mvc.perform(get("/api/adicionais/decimo/destinos")).andExpect(jsonPath("$.destinos", hasSize(1)))
                .andExpect(jsonPath("$.destinos[0].nome", is("N".repeat(60))));
    }

    // ---- projeção ------------------------------------------------------------------------------

    @Test
    void projecaoDivideOLiquidoDaParcela() throws Exception {
        JsonNode antes = projecao();
        String metaId = criarMeta("Viagem");
        putDestinos("decimo", """
                [{"nome": "Lazer", "tipo": "LAZER", "percentual": 50},
                 {"nome": "Viagem", "tipo": "RESERVA", "percentual": 30, "metaId": %s},
                 {"nome": "Colchão", "tipo": "RESERVA", "percentual": 10}]""".formatted(metaId))
                .andExpect(status().isOk());
        JsonNode proj = projecao();
        JsonNode out = proj.get("meses").get(0);
        assertThat(out.get("adicionais")).isEmpty();
        JsonNode nov = proj.get("meses").get(1);
        JsonNode a = nov.get("adicionais").get(0);
        assertThat(a.get("evento").asText()).isEqualTo("DECIMO");
        assertThat(v(a.get("liquido"))).isEqualByComparingTo(v(nov.get("decimo")).subtract(v(nov.get("descontoDecimo"))));
        BigDecimal lazer = v(a.get("destinos").get(0).get("valor"));
        BigDecimal viagem = v(a.get("destinos").get(1).get("valor"));
        BigDecimal colchao = v(a.get("destinos").get(2).get("valor"));
        assertThat(a.get("destinos").get(1).get("metaId").asText()).isEqualTo(metaId);
        assertThat(v(nov.get("gastosAdicionais"))).isEqualByComparingTo(lazer);
        assertThat(v(nov.get("reservasAdicionais"))).isEqualByComparingTo(viagem.add(colchao));
        assertThat(v(nov.get("guardadoReservaAdicionais"))).isEqualByComparingTo(colchao);
        assertThat(v(nov.get("sobra"))).isEqualByComparingTo(v(antes.get("meses").get(1).get("sobra"))
                .subtract(lazer).subtract(viagem).subtract(colchao));
        JsonNode metaNov = null;
        for (JsonNode m : nov.get("metas")) if (m.get("id").asText().equals(metaId)) metaNov = m;
        assertThat(v(metaNov.get("aporteAdicional"))).isEqualByComparingTo(viagem);
        assertThat(v(metaNov.get("guardado"))).isEqualByComparingTo(viagem);
        // PLR inativo: nada de PLR na projeção, mesmo com destinos
        putDestinos("plr", "[{\"nome\": \"Lazer\", \"tipo\": \"LAZER\", \"percentual\": 100}]").andExpect(status().isOk());
        for (JsonNode m : projecao().get("meses")) {
            for (JsonNode ad : m.get("adicionais")) assertThat(ad.get("evento").asText()).isEqualTo("DECIMO");
        }
        // resumo: totais por tipo
        JsonNode resumo = projecao().get("resumo");
        List<JsonNode> parcelas = new ArrayList<>();
        proj.get("meses").forEach(m -> m.get("adicionais").forEach(parcelas::add));
        BigDecimal lazerTotal = parcelas.stream().map(p -> v(p.get("destinos").get(0).get("valor")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(v(resumo.get("adicionais").get("total").get("lazer"))).isEqualByComparingTo(lazerTotal);
        assertThat(v(resumo.get("adicionais").get("decimo").get("lazer"))).isEqualByComparingTo(lazerTotal);
        assertThat(v(resumo.get("adicionais").get("plr").get("liquido"))).isEqualByComparingTo("0");
        assertThat(v(resumo.get("gastosAdicionais"))).isEqualByComparingTo(lazerTotal);
    }

    @Test
    void plrInativoTemPreviaMasMarcaAtivoFalse() throws Exception {
        putDestinos("plr", "[{\"nome\": \"Viagem\", \"tipo\": \"LAZER\", \"percentual\": 70}]").andExpect(status().isOk());
        JsonNode r = ler(mvc.perform(get("/api/adicionais/plr/destinos")).andExpect(status().isOk()));
        assertThat(r.get("ativo").asBoolean()).isFalse();
        assertThat(r.get("previa")).hasSize(2);
        assertThat(r.get("previa").get(0).get("mes").asText()).isEqualTo("2027-03");
        assertThat(r.get("previa").get(1).get("mes").asText()).isEqualTo("2027-09");
        // ativando, a projeção passa a dividir o PLR
        json(put("/api/plr"), "{\"ativo\": true}").andExpect(status().isOk());
        JsonNode mar = projecao().get("meses").get(5);
        assertThat(mar.get("competencia").asText()).isEqualTo("2027-03");
        JsonNode plr = null;
        for (JsonNode ad : mar.get("adicionais")) if (ad.get("evento").asText().equals("PLR")) plr = ad;
        assertThat(v(plr.get("liquido"))).isEqualByComparingTo(v(mar.get("plr")));
        assertThat(v(plr.get("destinos").get(0).get("valor")))
                .isEqualByComparingTo(v(r.get("previa").get(0).get("destinos").get(0).get("valor")));
    }

    @Test
    void metaApagadaViraReservaDosAdicionais() throws Exception {
        String metaId = criarMeta("Viagem");
        putDestinos("decimo", "[{\"nome\": \"Viagem\", \"tipo\": \"RESERVA\", \"percentual\": 40, \"metaId\": " + metaId + "}]")
                .andExpect(status().isOk());
        mvc.perform(delete("/api/metas/" + metaId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/adicionais/decimo/destinos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.destinos", hasSize(1)))
                .andExpect(jsonPath("$.destinos[0].metaId", nullValue()))
                .andExpect(jsonPath("$.destinos[0].metaNome", nullValue()))
                .andExpect(jsonPath("$.destinos[0].tipo", is("RESERVA")));
        JsonNode nov = projecao().get("meses").get(1);
        assertThat(v(nov.get("guardadoReservaAdicionais"))).isEqualByComparingTo(v(nov.get("reservasAdicionais")));
        assertThat(v(nov.get("reservasAdicionais"))).isPositive();
    }

    // ---- saúde e agregado ----------------------------------------------------------------------

    @Test
    void saudeTemBlocoAdicionais() throws Exception {
        JsonNode sem = ler(mvc.perform(get("/api/saude-financeira")).andExpect(status().isOk()));
        assertThat(sem.get("adicionais").get("definido").asBoolean()).isFalse();
        assertThat(sem.get("diagnostico").toString()).contains("Você ainda não definiu como usar seu 13º.");

        putDestinos("decimo", """
                [{"nome": "Lazer", "tipo": "LAZER", "percentual": 60}, {"nome": "IPVA", "tipo": "OBRIGATORIO", "percentual": 10},
                 {"nome": "Reserva", "tipo": "RESERVA", "percentual": 30}]""").andExpect(status().isOk());
        JsonNode s = ler(mvc.perform(get("/api/saude-financeira")).andExpect(status().isOk()));
        JsonNode ad = s.get("adicionais");
        BigDecimal total = v(ad.get("totalLiquidoPeriodo"));
        assertThat(total).isPositive();
        assertThat(v(ad.get("divisao").get("desejos")).add(v(ad.get("divisao").get("necessidades")))
                .add(v(ad.get("divisao").get("poupanca"))).add(v(ad.get("divisao").get("semDestino"))))
                .isEqualByComparingTo(total);
        assertThat(v(ad.get("percentuais").get("desejos"))).isEqualByComparingTo("60.0");
        assertThat(v(ad.get("percentuais").get("poupanca"))).isEqualByComparingTo("30.0");
        assertThat(ad.get("comparacao").get("status").get("desejos").asText()).isEqualTo("ACIMA");
        assertThat(ad.get("comparacao").get("status").get("poupanca").asText()).isEqualTo("OK");
        assertThat(ad.get("diagnostico").get(0).asText()).isEqualTo(
                "Dos seus adicionais nos próximos 12 meses (13º), 60% vão para lazer, 10% para gastos obrigatórios "
                        + "e 30% para reservas.");
        assertThat(s.get("diagnostico").toString()).contains("60% vão para lazer");
        // o 50/30/20 mensal não muda (renda de mês comum, aportes das metas)
        assertThat(v(s.get("atual").get("poupanca"))).isEqualByComparingTo(v(sem.get("atual").get("poupanca")));
        assertThat(v(s.get("rendaMensal"))).isEqualByComparingTo(v(sem.get("rendaMensal")));
    }

    @Test
    void dadosIncluemPlanosERestaurarLimpa() throws Exception {
        putDestinos("decimo", "[{\"nome\": \"Lazer\", \"tipo\": \"LAZER\", \"percentual\": 60}]").andExpect(status().isOk());
        mvc.perform(get("/api/dados")).andExpect(status().isOk())
                .andExpect(jsonPath("$.adicionais.decimo.destinos", hasSize(1)))
                .andExpect(jsonPath("$.adicionais.decimo.evento", is("DECIMO")))
                .andExpect(jsonPath("$.adicionais.plr.evento", is("PLR")))
                .andExpect(jsonPath("$.saudeFinanceira.adicionais.definido", is(true)));
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.adicionais.decimo.destinos", hasSize(0)))
                .andExpect(jsonPath("$.adicionais.plr.destinos", hasSize(0)));
    }
}
