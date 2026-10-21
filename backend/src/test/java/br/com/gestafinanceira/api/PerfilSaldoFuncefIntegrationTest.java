package br.com.gestafinanceira.api;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Saldo FUNCEF no perfil (V11) e previdência na projeção. Cada teste faz rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PerfilSaldoFuncefIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private ResultActions json(MockHttpServletRequestBuilder req, String body) throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode ler(ResultActions r) throws Exception {
        return om.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private ObjectNode perfil() throws Exception {
        return (ObjectNode) ler(mvc.perform(get("/api/perfil")).andExpect(status().isOk()));
    }

    @Test
    void saldoFuncefPadraoZeroSalvaEValida() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.saldoFuncef", is(0.0)));
        ObjectNode p = perfil();
        p.put("saldoFuncef", 15000.5);
        json(put("/api/perfil"), p.toString()).andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoFuncef", is(15000.5)));
        mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.saldoFuncef", is(15000.5)));

        // campo ausente mantém o atual
        p.remove("saldoFuncef");
        json(put("/api/perfil"), p.toString()).andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoFuncef", is(15000.5)));

        // negativo é rejeitado
        p.put("saldoFuncef", -1);
        json(put("/api/perfil"), p.toString()).andExpect(status().isBadRequest());
        mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.saldoFuncef", is(15000.5)));

        // restaurar padrão zera
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk());
        mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.saldoFuncef", is(0.0)));
    }

    @Test
    void previdenciaEvoluiNaProjecaoEResumo() throws Exception {
        ObjectNode p = perfil();
        p.put("funcefPct", 6);
        p.put("saldoFuncef", 20000);
        json(put("/api/perfil"), p.toString()).andExpect(status().isOk());

        JsonNode proj = ler(mvc.perform(get("/api/projecao?inicio=2026-01")).andExpect(status().isOk()));
        BigDecimal saldo = new BigDecimal("20000");
        for (JsonNode m : proj.get("meses")) {
            saldo = saldo.add(m.get("funcef").decimalValue()).add(m.get("funcefDecimo").decimalValue());
            assertEquals(0, saldo.compareTo(m.get("previdencia").decimalValue()), m.get("competencia").asText());
            assertEquals(0, m.get("patrimonio").decimalValue().add(m.get("previdencia").decimalValue())
                    .compareTo(m.get("patrimonioTotal").decimalValue()));
        }
        JsonNode r = proj.get("resumo");
        assertEquals(0, r.get("previdenciaInicial").decimalValue().compareTo(new BigDecimal("20000")));
        assertEquals(0, r.get("previdenciaFinal").decimalValue()
                .compareTo(proj.get("meses").get(11).get("previdencia").decimalValue()));
        assertEquals(0, r.get("patrimonioTotalFinal").decimalValue()
                .compareTo(r.get("patrimonioFinal").decimalValue().add(r.get("previdenciaFinal").decimalValue())));
        assertEquals(1, r.get("previdenciaFinal").decimalValue().compareTo(r.get("previdenciaInicial").decimalValue()));
    }

    @Test
    void semFuncefNemSaldoPrevidenciaFicaZerada() throws Exception {
        ObjectNode p = perfil();
        p.put("funcefPct", 0);
        p.put("saldoFuncef", 0);
        json(put("/api/perfil"), p.toString()).andExpect(status().isOk());
        JsonNode proj = ler(mvc.perform(get("/api/projecao?inicio=2026-01")).andExpect(status().isOk()));
        for (JsonNode m : proj.get("meses")) {
            assertEquals(0, m.get("previdencia").decimalValue().signum());
        }
        assertEquals(0, proj.at("/resumo/previdenciaFinal").decimalValue().signum());
        assertEquals(0, proj.at("/resumo/patrimonioTotalFinal").decimalValue()
                .compareTo(proj.at("/resumo/patrimonioFinal").decimalValue()));
    }
}
