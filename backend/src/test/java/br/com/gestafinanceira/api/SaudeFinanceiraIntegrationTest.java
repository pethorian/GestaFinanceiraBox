package br.com.gestafinanceira.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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

/** Saúde financeira (50/30/20), Clock fixo em 15/10/2026. Cada teste faz rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SaudeFinanceiraIntegrationTest {
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
        return om.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private JsonNode saude() throws Exception {
        return ler(mvc.perform(get("/api/saude-financeira")).andExpect(status().isOk()));
    }

    private void limparGastos() throws Exception {
        for (JsonNode l : ler(mvc.perform(get("/api/lancamentos")))) {
            mvc.perform(delete("/api/lancamentos/" + l.get("id").asText())).andExpect(status().isNoContent());
        }
    }

    private void gasto(String categoria, int valor) throws Exception {
        json(post("/api/lancamentos"), """
                {"descricao":"x","categoria":"%s","tipo":"RECORRENTE","valor":%d,"mesInicial":"2026-10","parcelas":0}"""
                .formatted(categoria, valor)).andExpect(status().isCreated());
    }

    private static BigDecimal v(JsonNode n) {
        return n.decimalValue();
    }

    @Test
    void migracaoV10MapeiaGruposIniciais() throws Exception {
        JsonNode cats = ler(mvc.perform(get("/api/categorias")).andExpect(status().isOk()));
        assertEquals(9, cats.size());
        for (JsonNode c : cats) {
            String esperado = switch (c.get("nome").asText()) {
                case "Pessoal", "Lazer", "Outros" -> "DESEJO";
                default -> "NECESSIDADE";
            };
            assertEquals(esperado, c.get("grupo").asText(), c.get("nome").asText());
        }
    }

    @Test
    void categoriaCriadaPadraoNecessidadeOuGrupoInformado() throws Exception {
        json(post("/api/categorias"), "{\"nome\":\"Educação\",\"ordem\":0}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.grupo", is("NECESSIDADE")));
        json(post("/api/categorias"), "{\"nome\":\"Streaming\",\"ordem\":0,\"grupo\":\"DESEJO\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.grupo", is("DESEJO")));
    }

    @Test
    void participaPadraoFalsoEPutAltera() throws Exception {
        mvc.perform(get("/api/saude-financeira")).andExpect(status().isOk())
                .andExpect(jsonPath("$.participa", is(false)))
                .andExpect(jsonPath("$.categorias", hasSize(9)));
        json(put("/api/saude-financeira"), "{\"participa\":true}").andExpect(status().isOk())
                .andExpect(jsonPath("$.participa", is(true)));
        mvc.perform(get("/api/saude-financeira")).andExpect(jsonPath("$.participa", is(true)));
        mvc.perform(get("/api/dados")).andExpect(jsonPath("$.saudeFinanceira.participa", is(true)));
        json(put("/api/saude-financeira"), "{}").andExpect(status().isBadRequest());
    }

    @Test
    void somasPorGrupoEPutGrupoMudaAsSomas() throws Exception {
        limparGastos();
        gasto("Moradia", 2000); // necessidade
        gasto("Lazer", 600);    // desejo
        JsonNode s = saude();
        assertEquals(0, v(s.at("/atual/necessidades")).compareTo(new BigDecimal("2000")));
        assertEquals(0, v(s.at("/atual/desejos")).compareTo(new BigDecimal("600")));
        BigDecimal renda = v(s.get("rendaMensal"));
        assertEquals(0, v(s.at("/recomendado/necessidades"))
                .compareTo(renda.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP)));

        json(put("/api/categorias/Lazer/grupo"), "{\"grupo\":\"NECESSIDADE\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", is("Lazer"))).andExpect(jsonPath("$.grupo", is("NECESSIDADE")));
        s = saude();
        assertEquals(0, v(s.at("/atual/necessidades")).compareTo(new BigDecimal("2600")));
        assertEquals(0, v(s.at("/atual/desejos")).signum());
        mvc.perform(get("/api/categorias")).andExpect(jsonPath("$[?(@.nome=='Lazer')].grupo", contains("NECESSIDADE")));
    }

    @Test
    void gastosExcluidosSaoIgnorados() throws Exception {
        limparGastos();
        gasto("Moradia", 1000);
        JsonNode s = saude();
        assertEquals(0, v(s.at("/atual/necessidades")).compareTo(new BigDecimal("1000")));
        String id = ler(mvc.perform(get("/api/lancamentos"))).get(0).get("id").asText();
        mvc.perform(delete("/api/lancamentos/" + id)).andExpect(status().isNoContent());
        s = saude();
        assertEquals(0, v(s.at("/atual/necessidades")).signum());
    }

    @Test
    void aportesDeMetasContamComoPoupancaESugestaoDiminui() throws Exception {
        limparGastos();
        for (JsonNode m : ler(mvc.perform(get("/api/metas")))) {
            mvc.perform(delete("/api/metas/" + m.get("id").asText())).andExpect(status().isNoContent());
        }
        JsonNode antes = saude();
        assertEquals(0, v(antes.at("/atual/poupanca")).signum());
        BigDecimal rec = v(antes.at("/recomendado/poupanca"));
        assertEquals(0, v(antes.get("aporteSugerido")).compareTo(rec));

        json(post("/api/metas"), "{\"nome\":\"Reserva\",\"valorAlvo\":0,\"valorAtual\":0,\"aporteMensal\":500}")
                .andExpect(status().isCreated());
        JsonNode s = saude();
        assertEquals(0, v(s.at("/atual/poupanca")).compareTo(new BigDecimal("500")));
        assertEquals(0, v(s.get("aporteSugerido"))
                .compareTo(rec.subtract(new BigDecimal("500")).max(BigDecimal.ZERO)));
    }

    @Test
    void restaurarPadraoVoltaParticipaEGrupos() throws Exception {
        json(put("/api/saude-financeira"), "{\"participa\":true}").andExpect(status().isOk());
        json(put("/api/categorias/Moradia/grupo"), "{\"grupo\":\"DESEJO\"}").andExpect(status().isOk());
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.saudeFinanceira.participa", is(false)));
        mvc.perform(get("/api/categorias")).andExpect(jsonPath("$[?(@.nome=='Moradia')].grupo", contains("NECESSIDADE")));
    }

    @Test
    void grupoDeCategoriaInexistenteOuInvalido() throws Exception {
        json(put("/api/categorias/Inexistente/grupo"), "{\"grupo\":\"DESEJO\"}").andExpect(status().isNotFound());
        json(put("/api/categorias/Lazer/grupo"), "{\"grupo\":\"TALVEZ\"}").andExpect(status().isBadRequest());
        json(put("/api/categorias/Lazer/grupo"), "{}").andExpect(status().isBadRequest());
    }
}
