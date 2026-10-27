package br.com.gestafinanceira.api;

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
import java.nio.charset.StandardCharsets;
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

/** Cartão de crédito (Clock fixo em 15/10/2026). Cada teste faz rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CartaoIntegrationTest {
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

    /** Cartão que fecha dia 10 e vence dia 20. */
    private long criarCartao(String limite) throws Exception {
        String corpo = """
                {"nome": "Visa", "diaFechamento": 10, "diaVencimento": 20, "limite": %s}""".formatted(limite);
        return ler(json(post("/api/cartoes"), corpo).andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions postCompra(long cartao, String tipo, String data, String valor, int parcelas, String cat)
            throws Exception {
        String corpo = """
                {"descricao": "Compra", "categoria": "%s", "tipo": "%s", "dataCompra": "%s", "valor": %s, "parcelas": %d}"""
                .formatted(cat, tipo, data, valor, parcelas);
        return json(post("/api/cartoes/" + cartao + "/compras"), corpo);
    }

    private BigDecimal gastoDoMes(int indice) throws Exception {
        JsonNode p = ler(mvc.perform(get("/api/projecao?inicio=2026-10")).andExpect(status().isOk()));
        return p.get("meses").get(indice).get("gastos").decimalValue();
    }

    private BigDecimal media(String query) throws Exception {
        JsonNode r = ler(mvc.perform(get("/api/lancamentos/resumo" + query)).andExpect(status().isOk()));
        return r.get("mediaMensalTotal").decimalValue();
    }

    @Test
    void crudDeCartaoEListagemComCalculados() throws Exception {
        long id = criarCartao("5000.00");
        mvc.perform(get("/api/cartoes")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id + ")].nome").value("Visa"))
                .andExpect(jsonPath("$[?(@.id==" + id + ")].limiteDisponivel").value(5000.0));

        json(put("/api/cartoes/" + id), """
                {"nome": "Mastercard", "diaFechamento": 5, "diaVencimento": 15, "limite": null}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Mastercard"))
                .andExpect(jsonPath("$.limite").isEmpty()).andExpect(jsonPath("$.limiteDisponivel").isEmpty());

        mvc.perform(delete("/api/cartoes/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/cartoes/" + id + "/compras")).andExpect(status().isNotFound());
        json(put("/api/cartoes/" + id), """
                {"nome": "X", "diaFechamento": 5, "diaVencimento": 15}""").andExpect(status().isNotFound());
    }

    @Test
    void validacoesDoCartaoEDaCompra() throws Exception {
        json(post("/api/cartoes"), """
                {"nome": "", "diaFechamento": 10, "diaVencimento": 20}""").andExpect(status().isBadRequest());
        json(post("/api/cartoes"), """
                {"nome": "%s", "diaFechamento": 10, "diaVencimento": 20}""".formatted("a".repeat(61)))
                .andExpect(status().isBadRequest());
        json(post("/api/cartoes"), """
                {"nome": "A", "diaFechamento": 0, "diaVencimento": 20}""").andExpect(status().isBadRequest());
        json(post("/api/cartoes"), """
                {"nome": "A", "diaFechamento": 10, "diaVencimento": 32}""").andExpect(status().isBadRequest());
        json(post("/api/cartoes"), """
                {"nome": "A", "diaFechamento": 10, "diaVencimento": 20, "limite": -1}""")
                .andExpect(status().isBadRequest());

        long id = criarCartao("null");
        postCompra(id, "A_VISTA", "2026-10-05", "0", 0, "Outros").andExpect(status().isBadRequest());
        postCompra(id, "A_VISTA", "2026-10-05", "10.123", 0, "Outros").andExpect(status().isBadRequest());
        postCompra(id, "PARCELADA", "2026-10-05", "100", 1, "Outros").andExpect(status().isBadRequest());
        postCompra(id, "PARCELADA", "2026-10-05", "100", 73, "Outros").andExpect(status().isBadRequest());
        postCompra(id, "PARCELADA", "2026-10-05", "100", 72, "Outros").andExpect(status().isCreated());
        postCompra(id, "PARCELADA", "2026-10-05", "100", 2, "Outros").andExpect(status().isCreated());
        postCompra(id, "OUTRO", "2026-10-05", "100", 0, "Outros").andExpect(status().isBadRequest());
        json(post("/api/cartoes/" + id + "/compras"), """
                {"descricao": "Sem data", "categoria": "Outros", "tipo": "A_VISTA", "valor": 10}""")
                .andExpect(status().isBadRequest());
        json(post("/api/cartoes/" + id + "/compras"), """
                {"descricao": "", "categoria": "Outros", "tipo": "A_VISTA", "dataCompra": "2026-10-05", "valor": 10}""")
                .andExpect(status().isBadRequest());
        postCompra(id, "A_VISTA", "2026-10-05", "10", 0, "Reserva").andExpect(status().isUnprocessableEntity());
        postCompra(9999, "A_VISTA", "2026-10-05", "10", 0, "Outros").andExpect(status().isNotFound());
    }

    @Test
    void parcelasSaoNormalizadasForaDeParcelada() throws Exception {
        long id = criarCartao("null");
        postCompra(id, "A_VISTA", "2026-10-05", "50.00", 12, "Outros").andExpect(status().isCreated())
                .andExpect(jsonPath("$.parcelas").value(0));
    }

    @Test
    void comprasTemCalculadosEFaturasMostramParcelasComResto() throws Exception {
        long id = criarCartao("null");
        // 5/10 (< fechamento 10): fecha em 10/out e vence em 20/out. 100,00 em 3x: 33,34 + 33,33 + 33,33
        JsonNode c = ler(postCompra(id, "PARCELADA", "2026-10-05", "100.00", 3, "Outros").andExpect(status().isCreated()));
        assertEquals("2026-10", c.get("primeiraFatura").asText());
        assertEquals("2026-12", c.get("ultimaFatura").asText());
        assertEquals(0, new BigDecimal("33.33").compareTo(c.get("valorParcela").decimalValue()));
        assertEquals(3, c.get("parcelasRestantes").asInt());

        JsonNode a = ler(postCompra(id, "ASSINATURA", "2026-10-12", "30.00", 0, "Lazer").andExpect(status().isCreated()));
        // dia 12 >= 10: cai na fatura que fecha em 10/nov e vence em 20/nov
        assertEquals("2026-11", a.get("primeiraFatura").asText());
        assertEquals(true, a.get("ultimaFatura").isNull());
        assertEquals(true, a.get("parcelasRestantes").isNull());

        mvc.perform(get("/api/cartoes/" + id + "/compras")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        JsonNode f = ler(mvc.perform(get("/api/cartoes/" + id + "/faturas")).andExpect(status().isOk()));
        assertEquals(12, f.size());
        assertEquals("2026-10", f.get(0).get("mes").asText());
        assertEquals(0, new BigDecimal("33.34").compareTo(f.get(0).get("total").decimalValue()));
        assertEquals("1/3", f.get(0).get("itens").get(0).get("parcela").asText());
        assertEquals(0, new BigDecimal("63.33").compareTo(f.get(1).get("total").decimalValue()));
        assertEquals(2, f.get(1).get("itens").size());
        assertEquals(0, new BigDecimal("30.00").compareTo(f.get(3).get("total").decimalValue()));

        JsonNode f2 = ler(mvc.perform(get("/api/cartoes/" + id + "/faturas?inicio=2027-01")).andExpect(status().isOk()));
        assertEquals("2027-01", f2.get(0).get("mes").asText());
        assertEquals(0, new BigDecimal("30.00").compareTo(f2.get(0).get("total").decimalValue()));
    }

    @Test
    void editarEExcluirCompra() throws Exception {
        long id = criarCartao("null");
        long compra = ler(postCompra(id, "A_VISTA", "2026-09-05", "100.00", 0, "Outros")).get("id").asLong();
        json(put("/api/cartoes/compras/" + compra), """
                {"descricao": "Editada", "categoria": "Lazer", "tipo": "PARCELADA", "dataCompra": "2026-10-12",
                 "valor": 200.00, "parcelas": 4}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Editada"))
                .andExpect(jsonPath("$.primeiraFatura").value("2026-11"))
                .andExpect(jsonPath("$.ultimaFatura").value("2027-02"));
        mvc.perform(delete("/api/cartoes/compras/" + compra)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/cartoes/compras/" + compra)).andExpect(status().isNotFound());
        mvc.perform(get("/api/cartoes/" + id + "/compras")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void projecaoIncluiAFaturaNoMesCerto() throws Exception {
        BigDecimal out = gastoDoMes(0), nov = gastoDoMes(1), dez = gastoDoMes(2);
        long id = criarCartao("null");
        // 5/out fecha em 10/out e vence em 20/out; 100,00 em 3x => out 33,34; nov e dez 33,33
        postCompra(id, "PARCELADA", "2026-10-05", "100.00", 3, "Outros").andExpect(status().isCreated());
        assertEquals(0, new BigDecimal("33.34").compareTo(gastoDoMes(0).subtract(out)));
        assertEquals(0, new BigDecimal("33.33").compareTo(gastoDoMes(1).subtract(nov)));
        assertEquals(0, new BigDecimal("33.33").compareTo(gastoDoMes(2).subtract(dez)));
        assertEquals(0, BigDecimal.ZERO.compareTo(gastoDoMes(3).subtract(gastoDoMes(3))));
    }

    @Test
    void resumoFixosIncluiOCartaoEEventuaisNao() throws Exception {
        String ini = "?inicio=2026-10";
        BigDecimal fixos0 = media(ini + "&grupo=FIXOS");
        BigDecimal eventuais0 = media(ini + "&grupo=EVENTUAIS");
        BigDecimal todos0 = media(ini);
        long id = criarCartao("null");
        postCompra(id, "A_VISTA", "2026-10-05", "1200.00", 0, "Outros").andExpect(status().isCreated()); // 100/mês
        postCompra(id, "ASSINATURA", "2026-09-01", "50.00", 0, "Lazer").andExpect(status().isCreated()); // 50/mês
        assertEquals(0, new BigDecimal("150.00").compareTo(media(ini + "&grupo=FIXOS").subtract(fixos0)));
        assertEquals(0, eventuais0.compareTo(media(ini + "&grupo=EVENTUAIS")));
        assertEquals(0, new BigDecimal("150.00").compareTo(media(ini).subtract(todos0)));
    }

    @Test
    void categoriaEmUsoPorCompraEBloqueadaEDepoisLiberada() throws Exception {
        long id = criarCartao("null");
        long compra = ler(postCompra(id, "A_VISTA", "2026-10-05", "10.00", 0, "Categoria Cartao Teste"))
                .get("id").asLong();
        mvc.perform(delete("/api/categorias/Categoria Cartao Teste")).andExpect(status().isUnprocessableEntity());
        mvc.perform(delete("/api/cartoes/compras/" + compra)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/categorias/Categoria Cartao Teste")).andExpect(status().isNoContent());
    }

    @Test
    void exclusaoLogicaDoCartaoTiraAsComprasDaProjecao() throws Exception {
        BigDecimal out = gastoDoMes(0);
        BigDecimal todos0 = media("?inicio=2026-10");
        long id = criarCartao("null");
        postCompra(id, "ASSINATURA", "2026-09-01", "80.00", 0, "Outros").andExpect(status().isCreated());
        assertEquals(0, new BigDecimal("80.00").compareTo(gastoDoMes(0).subtract(out)));
        mvc.perform(delete("/api/cartoes/" + id)).andExpect(status().isNoContent());
        assertEquals(0, out.compareTo(gastoDoMes(0)));
        assertEquals(0, todos0.compareTo(media("?inicio=2026-10")));
        mvc.perform(get("/api/cartoes")).andExpect(jsonPath("$[?(@.id==" + id + ")]").isEmpty());
    }

    @Test
    void limiteDisponivelEFaturasAtualEProxima() throws Exception {
        // hoje é 15/10: a fatura atual é a aberta (fecha em 10/11, vence em 20/11) e a próxima, a de dez;
        // a de out (fechou em 10/10, vence em 20/10) ainda está por pagar e entra no comprometido
        long id = criarCartao("1000.00");
        postCompra(id, "PARCELADA", "2026-10-05", "600.00", 3, "Outros").andExpect(status().isCreated()); // out, nov, dez
        postCompra(id, "ASSINATURA", "2026-10-12", "30.00", 0, "Lazer").andExpect(status().isCreated()); // a partir de nov
        postCompra(id, "A_VISTA", "2026-09-05", "70.00", 0, "Outros").andExpect(status().isCreated()); // venceu em 20/set
        mvc.perform(get("/api/cartoes")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id + ")].faturaAtual").value(230.0))
                .andExpect(jsonPath("$[?(@.id==" + id + ")].proximaFatura").value(230.0))
                .andExpect(jsonPath("$[?(@.id==" + id + ")].comprometido").value(600.0))
                .andExpect(jsonPath("$[?(@.id==" + id + ")].limiteDisponivel").value(400.0));
    }

    @Test
    void restaurarPadraoExcluiLogicamenteCartoes() throws Exception {
        long id = criarCartao("null");
        postCompra(id, "ASSINATURA", "2026-10-01", "80.00", 0, "Outros").andExpect(status().isCreated());
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk());
        mvc.perform(get("/api/cartoes")).andExpect(jsonPath("$[?(@.id==" + id + ")]").isEmpty());
    }
}
