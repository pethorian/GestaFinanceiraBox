package br.com.gestafinanceira.api;

import br.com.gestafinanceira.api.dto.Dtos.PlrControleDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrGastoDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrGastoEntradaDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrRecebimentoDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrRecebimentoEntradaDto;
import br.com.gestafinanceira.application.PlrControleService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controle do PLR recebido: recebimentos, gastos e resumo por destino. Validação no serviço (400/422). */
@RestController
@RequestMapping("/api/plr")
public class PlrControleController {
    private final PlrControleService controle;

    public PlrControleController(PlrControleService controle) {
        this.controle = controle;
    }

    @GetMapping("/controle")
    public PlrControleDto controle() {
        return controle.buscar();
    }

    @PostMapping("/recebimentos")
    public ResponseEntity<PlrRecebimentoDto> criarRecebimento(@RequestBody PlrRecebimentoEntradaDto d) {
        PlrRecebimentoDto criado = controle.criarRecebimento(d);
        return ResponseEntity.created(URI.create("/api/plr/recebimentos/" + criado.id())).body(criado);
    }

    @PutMapping("/recebimentos/{id}")
    public PlrRecebimentoDto atualizarRecebimento(@PathVariable Long id, @RequestBody PlrRecebimentoEntradaDto d) {
        return controle.atualizarRecebimento(id, d);
    }

    @DeleteMapping("/recebimentos/{id}")
    public ResponseEntity<Void> removerRecebimento(@PathVariable Long id) {
        controle.removerRecebimento(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/gastos")
    public ResponseEntity<PlrGastoDto> criarGasto(@RequestBody PlrGastoEntradaDto d) {
        PlrGastoDto criado = controle.criarGasto(d);
        return ResponseEntity.created(URI.create("/api/plr/gastos/" + criado.id())).body(criado);
    }

    @PutMapping("/gastos/{id}")
    public PlrGastoDto atualizarGasto(@PathVariable Long id, @RequestBody PlrGastoEntradaDto d) {
        return controle.atualizarGasto(id, d);
    }

    @DeleteMapping("/gastos/{id}")
    public ResponseEntity<Void> removerGasto(@PathVariable Long id) {
        controle.removerGasto(id);
        return ResponseEntity.noContent().build();
    }
}
