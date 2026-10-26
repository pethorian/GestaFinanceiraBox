package br.com.gestafinanceira.api;

import br.com.gestafinanceira.api.dto.Dtos.CartaoDto;
import br.com.gestafinanceira.api.dto.Dtos.CartaoEntradaDto;
import br.com.gestafinanceira.api.dto.Dtos.CompraDto;
import br.com.gestafinanceira.api.dto.Dtos.CompraEntradaDto;
import br.com.gestafinanceira.application.CartaoService;
import br.com.gestafinanceira.domain.calculo.cartao.Fatura;
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

/** Cartões de crédito, compras e faturas. */
@RestController
@RequestMapping("/api/cartoes")
public class CartaoController {
    private final CartaoService cartoes;

    public CartaoController(CartaoService cartoes) {
        this.cartoes = cartoes;
    }

    @GetMapping
    public List<CartaoDto> listar() {
        return cartoes.listar();
    }

    @PostMapping
    public ResponseEntity<CartaoDto> criar(@Valid @RequestBody CartaoEntradaDto d) {
        CartaoDto criado = cartoes.criar(d);
        return ResponseEntity.created(URI.create("/api/cartoes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public CartaoDto atualizar(@PathVariable Long id, @Valid @RequestBody CartaoEntradaDto d) {
        return cartoes.atualizar(id, d);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        cartoes.remover(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/compras")
    public List<CompraDto> compras(@PathVariable Long id) {
        return cartoes.listarCompras(id);
    }

    @PostMapping("/{id}/compras")
    public ResponseEntity<CompraDto> criarCompra(@PathVariable Long id, @Valid @RequestBody CompraEntradaDto d) {
        CompraDto criada = cartoes.criarCompra(id, d);
        return ResponseEntity.created(URI.create("/api/cartoes/" + id + "/compras")).body(criada);
    }

    @PutMapping("/compras/{compraId}")
    public CompraDto atualizarCompra(@PathVariable Long compraId, @Valid @RequestBody CompraEntradaDto d) {
        return cartoes.atualizarCompra(compraId, d);
    }

    @DeleteMapping("/compras/{compraId}")
    public ResponseEntity<Void> removerCompra(@PathVariable Long compraId) {
        cartoes.removerCompra(compraId);
        return ResponseEntity.noContent().build();
    }

    /** 12 faturas (por mês de vencimento) a partir de {@code inicio} (AAAA-MM); sem o parâmetro, o mês atual. */
    @GetMapping("/{id}/faturas")
    public List<Fatura> faturas(@PathVariable Long id, @RequestParam(required = false) YearMonth inicio) {
        return cartoes.faturas(id, inicio);
    }
}
