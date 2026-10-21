package br.com.gestafinanceira.api;

import br.com.gestafinanceira.api.dto.Dtos.ErroDto;
import br.com.gestafinanceira.application.Erros.DadosInvalidos;
import br.com.gestafinanceira.application.Erros.NaoEncontrado;
import br.com.gestafinanceira.application.Erros.RegraDeNegocio;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class TratadorErros {
    private static final Logger log = LoggerFactory.getLogger(TratadorErros.class);

    @ExceptionHandler(NaoEncontrado.class)
    ResponseEntity<ErroDto> naoEncontrado(NaoEncontrado e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
    }

    @ExceptionHandler(RegraDeNegocio.class)
    ResponseEntity<ErroDto> regra(RegraDeNegocio e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), List.of());
    }

    @ExceptionHandler(DadosInvalidos.class)
    ResponseEntity<ErroDto> dadosInvalidos(DadosInvalidos e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage(), e.detalhes());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroDto> validacao(MethodArgumentNotValidException e) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos.", detalhes);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErroDto> corpoInvalido(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição malformada ou com valor em formato inválido.", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroDto> integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade", e);
        return resposta(HttpStatus.CONFLICT, "Operação viola uma restrição de integridade dos dados.", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroDto> semRecurso(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado.", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroDto> metodoNaoSuportado(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado para este endereço.", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroDto> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno.", List.of());
    }

    private static ResponseEntity<ErroDto> resposta(HttpStatus status, String mensagem, List<String> detalhes) {
        return ResponseEntity.status(status).body(new ErroDto(status.value(), status.getReasonPhrase(), mensagem, detalhes));
    }
}
