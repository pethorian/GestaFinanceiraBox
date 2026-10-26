package br.com.gestafinanceira.application;

import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import br.com.gestafinanceira.infra.persistencia.CategoriaRepo;
import br.com.gestafinanceira.infra.persistencia.Entidades.Categoria;
import org.springframework.stereotype.Component;

/** Regra comum de categoria para quem registra gastos (lançamentos e compras de cartão). */
@Component
class CategoriaGasto {
    private final CategoriaRepo categorias;

    CategoriaGasto(CategoriaRepo categorias) {
        this.categorias = categorias;
    }

    /** A reserva tem aba própria (Metas); não existe como categoria de gasto. */
    static void recusarReservada(String nome) {
        if ("Reserva".equalsIgnoreCase(nome)) {
            throw new RegraDeNegocio("'Reserva' não é categoria de gasto: use a aba Metas.");
        }
    }

    /** Recusa "Reserva" e cria a categoria se ainda não existir. Devolve o nome aparado. */
    String garantir(String nome) {
        String cat = nome.trim();
        recusarReservada(cat);
        if (!categorias.existsById(cat)) {
            Categoria c = new Categoria();
            c.nome = cat;
            c.ordem = (int) categorias.count() + 1;
            categorias.save(c);
        }
        return cat;
    }
}
