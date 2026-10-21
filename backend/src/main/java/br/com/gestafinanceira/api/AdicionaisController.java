package br.com.gestafinanceira.api;

import br.com.gestafinanceira.api.dto.Dtos.DestinoEntradaDto;
import br.com.gestafinanceira.api.dto.Dtos.PlanoAdicionalDto;
import br.com.gestafinanceira.application.AdicionalService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Plano de uso do 13º ({@code decimo}) e do PLR ({@code plr}): destinos de cada adicional. */
@RestController
@RequestMapping("/api/adicionais")
public class AdicionaisController {
    private final AdicionalService adicionais;

    public AdicionaisController(AdicionalService adicionais) {
        this.adicionais = adicionais;
    }

    @GetMapping("/{evento}/destinos")
    public PlanoAdicionalDto destinos(@PathVariable String evento) {
        return adicionais.buscar(AdicionalService.evento(evento));
    }

    /** Substitui a lista inteira (vazia = sem destinos). Validação no serviço: 400 formato, 422 regra. */
    @PutMapping("/{evento}/destinos")
    public PlanoAdicionalDto substituir(@PathVariable String evento, @RequestBody List<DestinoEntradaDto> destinos) {
        return adicionais.substituir(AdicionalService.evento(evento), destinos);
    }
}
