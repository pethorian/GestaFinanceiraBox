package br.com.gestafinanceira.application;

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
}
