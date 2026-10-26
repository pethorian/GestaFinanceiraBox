package br.com.gestafinanceira.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
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

/** Controle do PLR recebido: CRUD, validações, vínculo com os destinos e ids preservados. Cada teste faz rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlrControleIntegrationTest {
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

    private JsonNode controle() throws Exception {
        return ler(mvc.perform(get("/api/plr/controle")).andExpect(status().isOk()));
    }

    private long receber(String data, String desc, String valor) throws Exception {
        return ler(json(post("/api/plr/recebimentos"), """
                {"data": "%s", "descricao": "%s", "valor": %s}""".formatted(data, desc, valor))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long gastar(String data, String desc, String valor, Long destinoId) throws Exception {
        return ler(json(post("/api/plr/gastos"), """
                {"data": "%s", "descricao": "%s", "valor": %s, "destinoId": %s}""".formatted(data, desc, valor, destinoId))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    /** Sugestão 80/20; devolve os ids na ordem [Gastos, Pessoal]. */
    private long[] criarDestinos() throws Exception {
        JsonNode r = ler(putDestinos("plr", """
                [{"nome": "Gastos", "tipo": "OBRIGATORIO", "percentual": 80},
                 {"nome": "Pessoal", "tipo": "LAZER", "percentual": 20}]""").andExpect(status().isOk()));
        return new long[] {r.get("destinos").get(0).get("id").asLong(), r.get("destinos").get(1).get("id").asLong()};
    }

    @BeforeEach
    void preparar() throws Exception {
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk());
    }

    @Test
    void vazioPorPadrao() throws Exception {
        JsonNode c = controle();
        assertThat(c.get("recebido").decimalValue()).isEqualByComparingTo("0");
        assertThat(c.get("destinos")).isEmpty();
        assertThat(c.get("recebimentos")).isEmpty();
        assertThat(c.get("gastos")).isEmpty();
        assertThat(c.get("semDestino").get("alocado").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void exemploDoUsuario() throws Exception {
        long[] ids = criarDestinos();
        receber("2026-10-20", "PLR 2026", "10000");
        gastar("2026-10-22", "Tênis", "350", ids[1]);

        JsonNode c = controle();
        assertThat(c.get("recebido").decimalValue()).isEqualByComparingTo("10000");
        assertThat(c.get("gasto").decimalValue()).isEqualByComparingTo("350");
        assertThat(c.get("saldo").decimalValue()).isEqualByComparingTo("9650");
        JsonNode pessoal = c.get("destinos").get(1);
        assertThat(pessoal.get("nome").asText()).isEqualTo("Pessoal");
        assertThat(pessoal.get("alocado").decimalValue()).isEqualByComparingTo("2000");
        assertThat(pessoal.get("saldo").decimalValue()).isEqualByComparingTo("1650");
        assertThat(pessoal.get("percentualUsado").decimalValue()).isEqualByComparingTo("17.50");
        assertThat(c.get("destinos").get(0).get("alocado").decimalValue()).isEqualByComparingTo("8000");
        assertThat(c.get("gastos").get(0).get("destinoNome").asText()).isEqualTo("Pessoal");
    }

    @Test
    void crudDeRecebimentos() throws Exception {
        long a = receber("2026-10-01", "  Antecipação  ", "4000.50");
        long b = receber("2026-10-20", "Saldo", "5999.50");
        JsonNode c = controle();
        assertThat(c.get("recebimentos")).hasSize(2);
        // data decrescente
        assertThat(c.get("recebimentos").get(0).get("id").asLong()).isEqualTo(b);
        assertThat(c.get("recebimentos").get(1).get("descricao").asText()).isEqualTo("Antecipação");
        assertThat(c.get("recebido").decimalValue()).isEqualByComparingTo("10000");

        json(put("/api/plr/recebimentos/" + a), """
                {"data": "2026-10-02", "descricao": "Antecipação 1", "valor": 1000}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.descricao").value("Antecipação 1"));
        assertThat(controle().get("recebido").decimalValue()).isEqualByComparingTo("6999.50");

        mvc.perform(delete("/api/plr/recebimentos/" + b)).andExpect(status().isNoContent());
        assertThat(controle().get("recebimentos")).hasSize(1);
        mvc.perform(delete("/api/plr/recebimentos/" + b)).andExpect(status().isNotFound());
        json(put("/api/plr/recebimentos/" + b), "{\"data\": \"2026-10-02\", \"descricao\": \"x\", \"valor\": 1}")
                .andExpect(status().isNotFound());
    }

    @Test
    void crudDeGastosEMudancaDeDestino() throws Exception {
        long[] ids = criarDestinos();
        receber("2026-10-20", "PLR", "10000");
        long g = gastar("2026-10-22", "Mercado", "500", ids[0]);
        long sem = ler(json(post("/api/plr/gastos"), """
                {"data": "2026-10-23", "descricao": "Avulso", "valor": 40.5}""")
                .andExpect(status().isCreated())).get("id").asLong();

        JsonNode c = controle();
        assertThat(c.get("gastos")).hasSize(2);
        assertThat(c.get("gastos").get(0).get("id").asLong()).isEqualTo(sem);
        assertThat(c.get("gastos").get(0).get("destinoNome").isNull()).isTrue();
        assertThat(c.get("semDestino").get("gasto").decimalValue()).isEqualByComparingTo("40.50");

        // move o gasto de Gastos para Pessoal
        json(put("/api/plr/gastos/" + g), """
                {"data": "2026-10-22", "descricao": "Mercado", "valor": 500, "destinoId": %d}""".formatted(ids[1]))
                .andExpect(status().isOk()).andExpect(jsonPath("$.destinoNome").value("Pessoal"));
        c = controle();
        assertThat(c.get("destinos").get(0).get("gasto").decimalValue()).isEqualByComparingTo("0");
        assertThat(c.get("destinos").get(1).get("gasto").decimalValue()).isEqualByComparingTo("500");

        mvc.perform(delete("/api/plr/gastos/" + g)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/plr/gastos/" + g)).andExpect(status().isNotFound());
        assertThat(controle().get("gastos")).hasSize(1);
    }

    @Test
    void validacoesDeRecebimento() throws Exception {
        String url = "/api/plr/recebimentos";
        json(post(url), "{\"descricao\": \"x\", \"valor\": 10}").andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"  \", \"valor\": 10}").andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"" + "D".repeat(121) + "\", \"valor\": 10}")
                .andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"" + "D".repeat(120) + "\", \"valor\": 10}")
                .andExpect(status().isCreated());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"x\"}").andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"x\", \"valor\": 0}").andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"x\", \"valor\": -5}").andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"x\", \"valor\": 10.001}")
                .andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"01/10/2026\", \"descricao\": \"x\", \"valor\": 10}").andExpect(status().isBadRequest());
        assertThat(controle().get("recebimentos")).hasSize(1);
    }

    @Test
    void validacoesDeGasto() throws Exception {
        String url = "/api/plr/gastos";
        json(post(url), "{\"descricao\": \"x\", \"valor\": 10}").andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"" + "D".repeat(161) + "\", \"valor\": 10}")
                .andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"" + "D".repeat(160) + "\", \"valor\": 10}")
                .andExpect(status().isCreated());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"x\", \"valor\": 0}").andExpect(status().isBadRequest());
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"x\", \"valor\": 1.234}").andExpect(status().isBadRequest());
        // destino inexistente ou de outro evento: 422
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"x\", \"valor\": 10, \"destinoId\": 999999}")
                .andExpect(status().isUnprocessableEntity());
        long decimoId = ler(putDestinos("decimo", "[{\"nome\": \"D\", \"tipo\": \"LAZER\", \"percentual\": 10}]")
                .andExpect(status().isOk())).get("destinos").get(0).get("id").asLong();
        json(post(url), "{\"data\": \"2026-10-01\", \"descricao\": \"x\", \"valor\": 10, \"destinoId\": " + decimoId + "}")
                .andExpect(status().isUnprocessableEntity());
        assertThat(controle().get("gastos")).hasSize(1);
    }

    @Test
    void putDeDestinosPreservaIds() throws Exception {
        long[] ids = criarDestinos();
        receber("2026-10-20", "PLR", "10000");
        gastar("2026-10-22", "Tênis", "350", ids[1]);

        // muda percentuais e ordem mantendo os ids: o gasto continua ligado ao Pessoal
        JsonNode r = ler(putDestinos("plr", """
                [{"id": %d, "nome": "Pessoal", "tipo": "LAZER", "percentual": 30},
                 {"id": %d, "nome": "Gastos", "tipo": "OBRIGATORIO", "percentual": 70}]""".formatted(ids[1], ids[0]))
                .andExpect(status().isOk()));
        assertThat(r.get("destinos").get(0).get("id").asLong()).isEqualTo(ids[1]);
        assertThat(r.get("destinos").get(1).get("id").asLong()).isEqualTo(ids[0]);
        JsonNode c = controle();
        assertThat(c.get("destinos").get(0).get("nome").asText()).isEqualTo("Pessoal");
        assertThat(c.get("destinos").get(0).get("alocado").decimalValue()).isEqualByComparingTo("3000");
        assertThat(c.get("destinos").get(0).get("gasto").decimalValue()).isEqualByComparingTo("350");

        // novo destino (sem id) é inserido; os existentes mantêm o id
        r = ler(putDestinos("plr", """
                [{"id": %d, "nome": "Pessoal", "tipo": "LAZER", "percentual": 30},
                 {"id": %d, "nome": "Gastos", "tipo": "OBRIGATORIO", "percentual": 50},
                 {"nome": "Reserva", "tipo": "RESERVA", "percentual": 20}]""".formatted(ids[1], ids[0]))
                .andExpect(status().isOk()));
        assertThat(r.get("destinos")).hasSize(3);
        assertThat(r.get("destinos").get(0).get("id").asLong()).isEqualTo(ids[1]);
        assertThat(r.get("destinos").get(2).get("id").asLong()).isNotIn(ids[0], ids[1]);
    }

    @Test
    void idsInvalidosNoPutDeDestinos() throws Exception {
        long[] ids = criarDestinos();
        putDestinos("plr", "[{\"id\": 999999, \"nome\": \"X\", \"tipo\": \"LAZER\", \"percentual\": 10}]")
                .andExpect(status().isUnprocessableEntity());
        // id de destino do 13º não vale para o PLR
        long decimoId = ler(putDestinos("decimo", "[{\"nome\": \"D\", \"tipo\": \"LAZER\", \"percentual\": 10}]")
                .andExpect(status().isOk())).get("destinos").get(0).get("id").asLong();
        putDestinos("plr", "[{\"id\": " + decimoId + ", \"nome\": \"X\", \"tipo\": \"LAZER\", \"percentual\": 10}]")
                .andExpect(status().isUnprocessableEntity());
        // id repetido
        putDestinos("plr", """
                [{"id": %d, "nome": "A", "tipo": "LAZER", "percentual": 10},
                 {"id": %d, "nome": "B", "tipo": "LAZER", "percentual": 10}]""".formatted(ids[0], ids[0]))
                .andExpect(status().isUnprocessableEntity());
        // nada mudou
        mvc.perform(get("/api/adicionais/plr/destinos")).andExpect(jsonPath("$.destinos", hasSize(2)));
    }

    @Test
    void naoRemoveDestinoComGastoAtivo() throws Exception {
        long[] ids = criarDestinos();
        long g = gastar("2026-10-22", "Tênis", "350", ids[1]);

        putDestinos("plr", "[{\"id\": %d, \"nome\": \"Gastos\", \"tipo\": \"OBRIGATORIO\", \"percentual\": 80}]".formatted(ids[0]))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("Há gastos do PLR no destino 'Pessoal'")));
        putDestinos("plr", "[]").andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/adicionais/plr/destinos")).andExpect(jsonPath("$.destinos", hasSize(2)));

        // apagado o gasto (exclusão lógica), o destino pode sair
        mvc.perform(delete("/api/plr/gastos/" + g)).andExpect(status().isNoContent());
        putDestinos("plr", "[{\"id\": %d, \"nome\": \"Gastos\", \"tipo\": \"OBRIGATORIO\", \"percentual\": 80}]".formatted(ids[0]))
                .andExpect(status().isOk()).andExpect(jsonPath("$.destinos", hasSize(1)));
    }

    @Test
    void destinoDoDecimoPodeSerRemovidoLivremente() throws Exception {
        putDestinos("decimo", "[{\"nome\": \"D\", \"tipo\": \"LAZER\", \"percentual\": 10}]").andExpect(status().isOk());
        putDestinos("decimo", "[]").andExpect(status().isOk()).andExpect(jsonPath("$.destinos", hasSize(0)));
    }

    @Test
    void restaurarPadraoExcluiRecebimentosEGastos() throws Exception {
        long[] ids = criarDestinos();
        receber("2026-10-20", "PLR", "10000");
        gastar("2026-10-22", "Tênis", "350", ids[1]);
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk());
        JsonNode c = controle();
        assertThat(c.get("recebimentos")).isEmpty();
        assertThat(c.get("gastos")).isEmpty();
        assertThat(c.get("recebido").decimalValue()).isEqualByComparingTo("0");
    }
}
