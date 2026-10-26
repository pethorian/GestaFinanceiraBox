package br.com.gestafinanceira.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Abas Gastos fixos e Gastos eventuais: criação por tipo e resumo por grupo. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GastosGrupoIntegrationTest {
    @Autowired MockMvc mvc;

    private BigDecimal media(String query) throws Exception {
        String r = mvc.perform(get("/api/lancamentos/resumo" + query)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new BigDecimal(r.replaceAll(".*\"mediaMensalTotal\":([0-9.]+).*", "$1"));
    }

    private void criar(String tipo, String mes, int parcelas, int valor) throws Exception {
        String corpo = """
                {"descricao": "G %s", "categoria": "Outros", "tipo": "%s", "valor": %d,
                 "mesInicial": "%s", "parcelas": %d}""".formatted(tipo, tipo, valor, mes, parcelas);
        mvc.perform(post("/api/lancamentos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
    }

    @Test
    void novoAceitaTipoEPadraoEhRecorrente() throws Exception {
        mvc.perform(post("/api/lancamentos/novo")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("RECORRENTE"));
        mvc.perform(post("/api/lancamentos/novo?tipo=PONTUAL")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PONTUAL"));
        mvc.perform(post("/api/lancamentos/novo?tipo=INEXISTENTE")).andExpect(status().isBadRequest());
    }

    @Test
    void resumoPorGrupoSeparaFixosDeEventuaisESemGrupoSomaTudo() throws Exception {
        String ini = "?inicio=2099-01";
        BigDecimal fixos0 = media(ini + "&grupo=FIXOS");
        BigDecimal eventuais0 = media(ini + "&grupo=EVENTUAIS");
        BigDecimal todos0 = media(ini);
        assertEquals(0, todos0.compareTo(fixos0.add(eventuais0)));

        criar("RECORRENTE", "2099-01", 0, 1200);   // 1200/mês
        criar("PARCELADO", "2099-01", 6, 600);     // 6 x 600 em 12 meses = 300/mês
        criar("PONTUAL", "2099-03", 0, 2400);      // 2400 / 12 = 200/mês

        assertEquals(0, new BigDecimal("1500.00").compareTo(media(ini + "&grupo=FIXOS").subtract(fixos0)));
        assertEquals(0, new BigDecimal("200.00").compareTo(media(ini + "&grupo=EVENTUAIS").subtract(eventuais0)));
        assertEquals(0, new BigDecimal("1700.00").compareTo(media(ini).subtract(todos0)));
    }

    @Test
    void grupoInvalidoDevolve400() throws Exception {
        mvc.perform(get("/api/lancamentos/resumo?grupo=XYZ")).andExpect(status().isBadRequest());
    }
}
