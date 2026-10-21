package br.com.gestafinanceira.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.gestafinanceira.infra.persistencia.Entidades.Lancamento;
import br.com.gestafinanceira.infra.persistencia.LancamentoRepo;
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
import org.springframework.transaction.annotation.Transactional;

/** Soft delete, expiração de gastos pontuais (Clock fixo em 15/03/2027) e ausência da categoria Reserva. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LancamentoSoftDeleteTest {
    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock clockFixo() {
            return Clock.fixed(Instant.parse("2027-03-15T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired LancamentoRepo repo;

    private Long criar(String desc, String tipo, String mes) throws Exception {
        String corpo = """
                {"descricao": "%s", "categoria": "Outros", "tipo": "%s", "valor": 1000,
                 "mesInicial": "%s", "parcelas": 0}""".formatted(desc, tipo, mes);
        String resp = mvc.perform(post("/api/lancamentos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.valueOf(resp.replaceAll(".*\"id\":([0-9]+).*", "$1"));
    }

    private ResultActions listar() throws Exception {
        return mvc.perform(get("/api/lancamentos")).andExpect(status().isOk());
    }

    @Test
    void deleteEhLogicoESomeDeListagemEResumo() throws Exception {
        Long id = criar("Gasto sumico", "RECORRENTE", "2027-03");
        listar().andExpect(jsonPath("$[?(@.descricao=='Gasto sumico')]", hasSize(1)));
        double antes = mediaTotal();

        mvc.perform(delete("/api/lancamentos/" + id)).andExpect(status().isNoContent());

        listar().andExpect(jsonPath("$[?(@.descricao=='Gasto sumico')]", hasSize(0)));
        mvc.perform(get("/api/lancamentos/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/lancamentos/" + id)).andExpect(status().isNotFound());
        assertEquals(antes - 1000.0, mediaTotal(), 0.011);
        mvc.perform(get("/api/dados")).andExpect(jsonPath("$.lancamentos[?(@.descricao=='Gasto sumico')]", hasSize(0)));

        Lancamento l = repo.findById(id).orElseThrow(); // a linha continua no banco
        assertNotNull(l.deletadoEm);
        assertEquals("USUARIO", l.motivoExclusao);
    }

    private double mediaTotal() throws Exception {
        String r = mvc.perform(get("/api/lancamentos/resumo?inicio=2027-03")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Double.parseDouble(r.replaceAll(".*\"mediaMensalTotal\":([0-9.]+).*", "$1"));
    }

    @Test
    void pontualDeMesAnteriorExpiraEOutrosPermanecem() throws Exception {
        Long passado = criar("Pontual passado", "PONTUAL", "2027-02");
        Long atual = criar("Pontual atual", "PONTUAL", "2027-03");
        Long futuro = criar("Pontual futuro", "PONTUAL", "2027-04");
        Long recorrenteAntigo = criar("Recorrente antigo", "RECORRENTE", "2025-01");
        Long parceladoAntigo = criar("Parcelado antigo", "PARCELADO", "2025-01");

        listar().andExpect(jsonPath("$[?(@.descricao=='Pontual passado')]", hasSize(0)))
                .andExpect(jsonPath("$[?(@.descricao=='Pontual atual')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.descricao=='Pontual futuro')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.descricao=='Recorrente antigo')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.descricao=='Parcelado antigo')]", hasSize(1)));
        mvc.perform(get("/api/lancamentos/" + passado)).andExpect(status().isNotFound());

        Lancamento l = repo.findById(passado).orElseThrow();
        assertNotNull(l.deletadoEm);
        assertEquals("EXPIRADO", l.motivoExclusao);
        for (Long id : new Long[] {atual, futuro, recorrenteAntigo, parceladoAntigo}) {
            assertNull(repo.findById(id).orElseThrow().deletadoEm);
        }
        // idempotente: nova leitura não altera o motivo/instante
        var primeiro = l.deletadoEm;
        listar();
        assertEquals(primeiro, repo.findById(passado).orElseThrow().deletadoEm);
    }

    @Test
    void projecaoEDadosIgnoramPontualVencido() throws Exception {
        Long passado = criar("Pontual passado", "PONTUAL", "2027-01");
        mvc.perform(get("/api/projecao?inicio=2027-01")).andExpect(status().isOk());
        assertEquals("EXPIRADO", repo.findById(passado).orElseThrow().motivoExclusao);
    }

    @Test
    void restaurarPadraoFazSoftDeleteDosAtivos() throws Exception {
        Long id = criar("Para restaurar", "RECORRENTE", "2027-03");
        mvc.perform(post("/api/dados/restaurar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lancamentos", hasSize(7)));
        Lancamento l = repo.findById(id).orElseThrow();
        assertNotNull(l.deletadoEm);
        assertEquals("USUARIO", l.motivoExclusao);
    }

    @Test
    void categoriaReservaNaoExisteMais() throws Exception {
        mvc.perform(get("/api/categorias")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nome=='Reserva')]", hasSize(0)))
                .andExpect(jsonPath("$[?(@.nome=='Outros')]", hasSize(1)));
        mvc.perform(post("/api/lancamentos").contentType(MediaType.APPLICATION_JSON).content("""
                {"descricao": "x", "categoria": "Reserva", "tipo": "RECORRENTE", "valor": 10,
                 "mesInicial": "2027-03", "parcelas": 0}""")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/lancamentos")).andExpect(jsonPath("$[?(@.categoria=='Reserva')]", hasSize(0)));
        mvc.perform(get("/api/metas")).andExpect(jsonPath("$[0].nome", is("Reserva de emergência")));
    }
}
