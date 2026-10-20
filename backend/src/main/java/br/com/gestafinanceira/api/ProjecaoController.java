package br.com.gestafinanceira.api;

import br.com.gestafinanceira.api.dto.Dtos.ProjecaoDto;
import br.com.gestafinanceira.application.ProjecaoService;
import br.com.gestafinanceira.domain.calculo.projecao.MesProjecao;
import java.time.YearMonth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projecao")
public class ProjecaoController {
    private final ProjecaoService projecao;

    public ProjecaoController(ProjecaoService projecao) {
        this.projecao = projecao;
    }

    /** 12 meses a partir de {@code inicio} (AAAA-MM); sem o parâmetro, o mês atual. */
    @GetMapping
    public ProjecaoDto projetar(@RequestParam(required = false) YearMonth inicio) {
        return projecao.projetar(inicio);
    }

    /** Mês "comum" (sem 13º e férias) para a memória de cálculo. */
    @GetMapping("/mes-comum")
    public MesProjecao mesComum(@RequestParam(required = false) YearMonth mes) {
        return projecao.mesComum(mes);
    }
}
