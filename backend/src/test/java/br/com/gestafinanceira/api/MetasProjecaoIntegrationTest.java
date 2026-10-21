package br.com.gestafinanceira.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.gestafinanceira.infra.persistencia.MetaRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

/** Campo banco das metas e aportes na projeção (Clock fixo em 15/10/2026). Cada teste faz rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MetasProjecaoIntegrationTest {
    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock clockFixo() {
            return Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired MetaRepo metas;
    @Autowired ObjectMapper om;

    private ResultActions json(MockHttpServletRequestBuilder req, String body) throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode ler(ResultActions r) throws Exception {
        return om.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private String criarMeta(String corpo) throws Exception {
        return ler(json(post("/api/metas"), corpo).andExpect(status().isCreated())).get("id").asText();
    }

    private JsonNode projecao() throws Exception {
        return ler(mvc.perform(get("/api/projecao?inicio=2026-10")).andExpect(status().isOk()));
    }

    private static BigDecimal v(JsonNode n) {
        return n.decimalValue();
    }

    // ---- banco ---------------------------------------------------------------------------------

    @Test
    void bancoESalvoAparadoELimpo() throws Exception {
        String id = criarMeta("""
                {"nome": "Reserva", "valorAlvo": 1000, "valorAtual": 0, "aporteMensal": 100,
                 "banco": "  Caixa Econômica Federal  "}""");
        mvc.perform(get("/api/metas/" + id)).andExpect(jsonPath("$.banco", is("Caixa Econômica Federal")));
        assertEquals("Caixa Econômica Federal", metas.findById(Long.valueOf(id)).orElseThrow().banco);

        json(put("/api/metas/" + id), """
                {"nome": "Reserva", "valorAlvo": 1000, "valorAtual": 0, "aporteMensal": 100, "banco": "   "}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.banco", nullValue()));
        assertEquals(null, metas.findById(Long.valueOf(id)).orElseThrow().banco);

        json(put("/api/metas/" + id), """
                {"nome": "Reserva", "valorAlvo": 1000, "valorAtual": 0, "aporteMensal": 100, "banco": "Nubank"}""")
                .andExpect(jsonPath("$.banco", is("Nubank")));
        // sem o campo: a meta é substituída inteira, banco volta a null
        json(put("/api/metas/" + id), """
                {"nome": "Reserva", "valorAlvo": 1000, "valorAtual": 0, "aporteMensal": 100}""")
                .andExpect(jsonPath("$.banco", nullValue()));
        mvc.perform(get("/api/dados")).andExpect(jsonPath("$.metas[?(@.id==" + id + ")].banco").exists());
    }

    @Test
    void bancoAte80CaracteresDepoisDeAparar() throws Exception {
        String oitenta = "B".repeat(80);
        criarMetaComBanco("  " + oitenta + "  ").andExpect(status().isCreated())
                .andExpect(jsonPath("$.banco", is(oitenta)));
        criarMetaComBanco(oitenta + "B").andExpect(status().isBadRequest());
    }

    private ResultActions criarMetaComBanco(String banco) throws Exception {
        return json(post("/api/metas"), """
                {"nome": "X", "valorAlvo": 1000, "valorAtual": 0, "aporteMensal": 100, "banco": "%s"}""".formatted(banco));
    }

    // ---- previsão --------------------------------------------------------------------------------

    @Test
    void previsaoEOMesDoUltimoAporteContandoOMesAtual() throws Exception {
        String id = criarMeta("""
                {"nome": "Viagem", "valorAlvo": 10000, "valorAtual": 1000, "aporteMensal": 1500}""");
        mvc.perform(get("/api/metas/" + id))
                .andExpect(jsonPath("$.mesesParaAtingir", is(6)))
                .andExpect(jsonPath("$.previsao", is("2027-03"))) // out, nov, dez, jan, fev, mar
                .andExpect(jsonPath("$.guardadoFimProjecao", is(10000.0)))
                .andExpect(jsonPath("$.fimProjecao", is("2027-09")));
        String semAlvo = criarMeta("""
                {"nome": "Livre", "valorAlvo": 0, "valorAtual": 100, "aporteMensal": 50}""");
        mvc.perform(get("/api/metas/" + semAlvo))
                .andExpect(jsonPath("$.mesesParaAtingir", nullValue()))
                .andExpect(jsonPath("$.previsao", nullValue()))
                .andExpect(jsonPath("$.guardadoFimProjecao", is(700.0)));
    }

    // ---- projeção --------------------------------------------------------------------------------

    @Test
    void projecaoDebitaAportesEPreservaOPatrimonio() throws Exception {
        metas.deleteAll();
        JsonNode sem = projecao();
        criarMeta("""
                {"nome": "Reserva", "valorAlvo": 10000, "valorAtual": 1000, "aporteMensal": 2000, "banco": "Caixa"}""");
        JsonNode com = projecao();

        String[] aportes = {"2000", "2000", "2000", "2000", "1000", "0", "0", "0", "0", "0", "0", "0"};
        for (int i = 0; i < 12; i++) {
            JsonNode c = com.get("meses").get(i);
            JsonNode s = sem.get("meses").get(i);
            assertEquals(0, v(c.get("aportes")).compareTo(new BigDecimal(aportes[i])), "aporte do mês " + i);
            assertEquals(0, v(c.get("sobra")).compareTo(v(c.get("liquido")).subtract(v(c.get("gastos"))).subtract(v(c.get("aportes")))));
            assertEquals(0, v(c.get("sobra")).compareTo(v(s.get("sobra")).subtract(v(c.get("aportes")))), "sobra reduzida");
            assertEquals(0, v(c.get("patrimonio")).compareTo(v(c.get("acumulado")).add(v(c.get("guardado")))));
        }
        JsonNode fev = com.get("meses").get(4);
        assertEquals("2027-02", fev.get("competencia").asText());
        assertEquals(0, v(fev.get("guardado")).compareTo(new BigDecimal("10000")));
        assertEquals("Reserva", fev.get("metas").get(0).get("nome").asText());

        JsonNode r = com.get("resumo");
        assertEquals(0, v(r.get("aportes")).compareTo(new BigDecimal("9000")));
        assertEquals(0, v(r.get("guardadoInicial")).compareTo(new BigDecimal("1000")));
        assertEquals(0, v(r.get("guardadoFinal")).compareTo(new BigDecimal("10000")));
        assertEquals(0, v(r.get("saldoFinal")).compareTo(v(sem.get("resumo").get("saldoFinal")).subtract(new BigDecimal("9000"))));
        assertEquals(0, v(r.get("patrimonioFinal")).compareTo(v(r.get("saldoFinal")).add(new BigDecimal("10000"))));
        assertEquals(0, v(r.get("sobra")).compareTo(v(sem.get("resumo").get("sobra")).subtract(new BigDecimal("9000"))));
        assertEquals(0, v(sem.get("resumo").get("aportes")).signum());
        assertEquals(0, v(sem.get("resumo").get("patrimonioFinal")).compareTo(v(sem.get("resumo").get("saldoFinal"))));
    }

    @Test
    void mesComumTambemDescontaOAporteDoMes() throws Exception {
        metas.deleteAll();
        criarMeta("""
                {"nome": "Reserva", "valorAlvo": 10000, "valorAtual": 9500, "aporteMensal": 1500}""");
        JsonNode m = ler(mvc.perform(get("/api/projecao/mes-comum?mes=2026-10")).andExpect(status().isOk()));
        assertEquals(0, v(m.get("aportes")).compareTo(new BigDecimal("500")));
        assertEquals(0, v(m.get("sobra")).compareTo(v(m.get("liquido")).subtract(v(m.get("gastos"))).subtract(new BigDecimal("500"))));
    }

    @Test
    void removerMetaTiraOAporteDaProjecao() throws Exception {
        metas.deleteAll();
        String id = criarMeta("""
                {"nome": "Reserva", "valorAlvo": 0, "valorAtual": 0, "aporteMensal": 300}""");
        assertEquals(0, v(projecao().get("resumo").get("aportes")).compareTo(new BigDecimal("3600")));
        mvc.perform(delete("/api/metas/" + id)).andExpect(status().isNoContent());
        assertEquals(0, v(projecao().get("resumo").get("aportes")).signum());
    }
}
