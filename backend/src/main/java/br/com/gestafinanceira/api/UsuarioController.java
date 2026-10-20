package br.com.gestafinanceira.api;

import br.com.gestafinanceira.api.dto.Dtos.BonusDto;
import br.com.gestafinanceira.api.dto.Dtos.CategoriaDto;
import br.com.gestafinanceira.api.dto.Dtos.DadosDto;
import br.com.gestafinanceira.api.dto.Dtos.DecimoDto;
import br.com.gestafinanceira.api.dto.Dtos.FeriasDto;
import br.com.gestafinanceira.api.dto.Dtos.LancamentoDto;
import br.com.gestafinanceira.api.dto.Dtos.MetaDto;
import br.com.gestafinanceira.api.dto.Dtos.MetaEntradaDto;
import br.com.gestafinanceira.api.dto.Dtos.PerfilDto;
import br.com.gestafinanceira.api.dto.Dtos.PlrDto;
import br.com.gestafinanceira.api.dto.Dtos.ResumoGastosDto;
import br.com.gestafinanceira.application.DadosService;
import br.com.gestafinanceira.application.DecimoService;
import br.com.gestafinanceira.application.LancamentoService;
import br.com.gestafinanceira.application.MetaService;
import br.com.gestafinanceira.application.PerfilService;
import br.com.gestafinanceira.application.PlrService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.YearMonth;
import java.util.List;
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

/** Dados do usuário: perfil, férias, 13º, gastos, metas e visão agregada. */
@RestController
@RequestMapping("/api")
public class UsuarioController {
    private final PerfilService perfil;
    private final LancamentoService lancamentos;
    private final DecimoService decimo;
    private final MetaService metas;
    private final DadosService dados;
    private final PlrService plr;

    public UsuarioController(PerfilService perfil, LancamentoService lancamentos, DecimoService decimo,
                             MetaService metas, DadosService dados, PlrService plr) {
        this.plr = plr;
        this.perfil = perfil;
        this.lancamentos = lancamentos;
        this.decimo = decimo;
        this.metas = metas;
        this.dados = dados;
    }

    // perfil e férias
    @GetMapping("/perfil")
    public PerfilDto perfil() {
        return perfil.buscar();
    }

    @PutMapping("/perfil")
    public PerfilDto atualizarPerfil(@Valid @RequestBody PerfilDto d) {
        return perfil.atualizar(d);
    }

    @GetMapping("/ferias")
    public FeriasDto ferias() {
        return perfil.buscarFerias();
    }

    @PutMapping("/ferias")
    public FeriasDto atualizarFerias(@Valid @RequestBody FeriasDto d) {
        return perfil.atualizarFerias(d);
    }

    // 13º
    @GetMapping("/decimo")
    public DecimoDto decimo() {
        return decimo.buscar();
    }

    @PostMapping("/decimo/parcelas")
    public DecimoDto novaParcelaDecimo() {
        return decimo.adicionarParcela();
    }

    @PutMapping("/decimo")
    public DecimoDto atualizarDecimo(@Valid @RequestBody DecimoDto d) {
        return decimo.substituir(d.parcelas());
    }

    // PLR
    @GetMapping("/plr")
    public PlrDto plr() {
        return plr.buscar();
    }

    @PutMapping("/plr")
    public PlrDto atualizarPlr(@Valid @RequestBody PlrDto d) {
        return plr.substituir(d.parcelas());
    }

    @PostMapping("/plr/parcelas")
    public PlrDto novaParcelaPlr() {
        return plr.adicionarParcela();
    }

    // bônus Caixa
    @GetMapping("/bonus")
    public BonusDto bonus() {
        return perfil.buscarBonus();
    }

    @PutMapping("/bonus")
    public BonusDto atualizarBonus(@Valid @RequestBody BonusDto d) {
        return perfil.atualizarBonus(d);
    }

    // lançamentos (gastos)
    @GetMapping("/lancamentos")
    public List<LancamentoDto> lancamentos() {
        return lancamentos.listar();
    }

    @GetMapping("/lancamentos/{id}")
    public LancamentoDto lancamento(@PathVariable Long id) {
        return lancamentos.buscar(id);
    }

    @GetMapping("/lancamentos/resumo")
    public ResumoGastosDto resumoLancamentos(@RequestParam(required = false) YearMonth inicio) {
        return lancamentos.resumoPorCategoria(inicio);
    }

    @PostMapping("/lancamentos/novo")
    public ResponseEntity<LancamentoDto> novoLancamento() {
        LancamentoDto criado = lancamentos.criarNovo();
        return ResponseEntity.created(URI.create("/api/lancamentos/" + criado.id())).body(criado);
    }

    @PostMapping("/lancamentos")
    public ResponseEntity<LancamentoDto> criarLancamento(@Valid @RequestBody LancamentoDto d) {
        LancamentoDto criado = lancamentos.criar(d);
        return ResponseEntity.created(URI.create("/api/lancamentos/" + criado.id())).body(criado);
    }

    @PutMapping("/lancamentos/{id}")
    public LancamentoDto atualizarLancamento(@PathVariable Long id, @Valid @RequestBody LancamentoDto d) {
        return lancamentos.atualizar(id, d);
    }

    @DeleteMapping("/lancamentos/{id}")
    public ResponseEntity<Void> removerLancamento(@PathVariable Long id) {
        lancamentos.remover(id);
        return ResponseEntity.noContent().build();
    }

    // categorias
    @GetMapping("/categorias")
    public List<CategoriaDto> categorias() {
        return lancamentos.listarCategorias();
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaDto> criarCategoria(@Valid @RequestBody CategoriaDto d) {
        return ResponseEntity.status(201).body(lancamentos.criarCategoria(d));
    }

    @DeleteMapping("/categorias/{nome}")
    public ResponseEntity<Void> removerCategoria(@PathVariable String nome) {
        lancamentos.removerCategoria(nome);
        return ResponseEntity.noContent().build();
    }

    // metas
    @GetMapping("/metas")
    public List<MetaDto> metas() {
        return metas.listar();
    }

    @GetMapping("/metas/{id}")
    public MetaDto meta(@PathVariable Long id) {
        return metas.buscar(id);
    }

    @PostMapping("/metas/novo")
    public ResponseEntity<MetaDto> novaMeta() {
        MetaDto criada = metas.criarNova();
        return ResponseEntity.created(URI.create("/api/metas/" + criada.id())).body(criada);
    }

    @PostMapping("/metas")
    public ResponseEntity<MetaDto> criarMeta(@Valid @RequestBody MetaEntradaDto d) {
        MetaDto criada = metas.criar(d);
        return ResponseEntity.created(URI.create("/api/metas/" + criada.id())).body(criada);
    }

    @PutMapping("/metas/{id}")
    public MetaDto atualizarMeta(@PathVariable Long id, @Valid @RequestBody MetaEntradaDto d) {
        return metas.atualizar(id, d);
    }

    @DeleteMapping("/metas/{id}")
    public ResponseEntity<Void> removerMeta(@PathVariable Long id) {
        metas.remover(id);
        return ResponseEntity.noContent().build();
    }

    // agregado
    @GetMapping("/dados")
    public DadosDto dados() {
        return dados.buscar();
    }

    @PostMapping("/dados/restaurar")
    public DadosDto restaurar() {
        return dados.restaurarPadrao();
    }
}
