package br.com.gestafinanceira.api;

import br.com.gestafinanceira.api.dto.Dtos.BeneficioDto;
import br.com.gestafinanceira.api.dto.Dtos.CargoDto;
import br.com.gestafinanceira.api.dto.Dtos.ParametrosVigentesDto;
import br.com.gestafinanceira.api.dto.Dtos.TabelaDeltasDto;
import br.com.gestafinanceira.api.dto.Dtos.TabelaFaixasDto;
import br.com.gestafinanceira.application.ParametrosService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tabelas com vigência. Os PUT de tabelas gravam (criam ou substituem) uma vigência inteira, identificada
 * por {@code vigenciaInicio}; o fim de cada vigência é recalculado automaticamente.
 */
@RestController
@RequestMapping("/api/parametros")
public class ParametrosController {
    private final ParametrosService parametros;

    public ParametrosController(ParametrosService parametros) {
        this.parametros = parametros;
    }

    @GetMapping("/vigentes")
    public ParametrosVigentesDto vigentes(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return parametros.vigentes(data);
    }

    // cargos (valores vigentes na data informada; padrão: hoje)
    @GetMapping("/cargos")
    public List<CargoDto> cargos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return parametros.listarCargos(data);
    }

    @GetMapping("/cargos/{id}")
    public CargoDto cargo(@PathVariable Long id) {
        return parametros.buscarCargo(id);
    }

    @PostMapping("/cargos")
    public ResponseEntity<CargoDto> criarCargo(@Valid @RequestBody CargoDto d) {
        CargoDto criado = parametros.criarCargo(d);
        return ResponseEntity.created(URI.create("/api/parametros/cargos/" + criado.id())).body(criado);
    }

    @PutMapping("/cargos/{id}")
    public CargoDto atualizarCargo(@PathVariable Long id, @Valid @RequestBody CargoDto d) {
        return parametros.atualizarCargo(id, d);
    }

    @DeleteMapping("/cargos/{id}")
    public ResponseEntity<Void> removerCargo(@PathVariable Long id) {
        parametros.removerCargo(id);
        return ResponseEntity.noContent().build();
    }

    // deltas
    @GetMapping("/deltas")
    public List<TabelaDeltasDto> deltas() {
        return parametros.listarDeltas();
    }

    @PutMapping("/deltas")
    public List<TabelaDeltasDto> salvarDeltas(@Valid @RequestBody TabelaDeltasDto d) {
        return parametros.salvarDeltas(d);
    }

    @DeleteMapping("/deltas/{inicio}")
    public List<TabelaDeltasDto> removerDeltas(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio) {
        return parametros.removerDeltas(inicio);
    }

    // INSS
    @GetMapping("/inss")
    public List<TabelaFaixasDto> inss() {
        return parametros.listarInss();
    }

    @PutMapping("/inss")
    public List<TabelaFaixasDto> salvarInss(@Valid @RequestBody TabelaFaixasDto d) {
        return parametros.salvarInss(d);
    }

    @DeleteMapping("/inss/{inicio}")
    public List<TabelaFaixasDto> removerInss(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio) {
        return parametros.removerInss(inicio);
    }

    // IRPF
    @GetMapping("/irpf")
    public List<TabelaFaixasDto> irpf() {
        return parametros.listarIrpf();
    }

    @PutMapping("/irpf")
    public List<TabelaFaixasDto> salvarIrpf(@Valid @RequestBody TabelaFaixasDto d) {
        return parametros.salvarIrpf(d);
    }

    @DeleteMapping("/irpf/{inicio}")
    public List<TabelaFaixasDto> removerIrpf(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio) {
        return parametros.removerIrpf(inicio);
    }

    // benefícios e demais parâmetros escalares
    @GetMapping("/beneficios")
    public List<BeneficioDto> beneficios() {
        return parametros.listarBeneficios();
    }

    @PutMapping("/beneficios")
    public List<BeneficioDto> salvarBeneficio(@Valid @RequestBody BeneficioDto d) {
        return parametros.salvarBeneficio(d);
    }

    @DeleteMapping("/beneficios/{inicio}")
    public List<BeneficioDto> removerBeneficio(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio) {
        return parametros.removerBeneficio(inicio);
    }
}
