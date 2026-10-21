package br.com.gestafinanceira.application;

import java.util.List;

/** Exceções de aplicação mapeadas para HTTP pelo handler da camada api. */
public final class Erros {
    private Erros() {}

    /** Recurso inexistente (404). */
    public static class NaoEncontrado extends RuntimeException {
        public NaoEncontrado(String mensagem) {
            super(mensagem);
        }
    }

    /** Regra de negócio violada (422). */
    public static class RegraDeNegocio extends RuntimeException {
        public RegraDeNegocio(String mensagem) {
            super(mensagem);
        }
    }

    /** Dados de entrada inválidos detectados no serviço (400), com um detalhe por campo. */
    public static class DadosInvalidos extends RuntimeException {
        private final List<String> detalhes;

        public DadosInvalidos(List<String> detalhes) {
            super("Dados inválidos.");
            this.detalhes = List.copyOf(detalhes);
        }

        public List<String> detalhes() {
            return detalhes;
        }
    }
}
