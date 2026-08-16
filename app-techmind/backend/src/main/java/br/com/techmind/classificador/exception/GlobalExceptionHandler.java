package br.com.techmind.classificador.exception;

import br.com.techmind.classificador.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/**
 * @author Diego Pitoco
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(MlIntegrationException.class)
    public ResponseEntity<ErroResponse> ml(MlIntegrationException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_GATEWAY, "Erro de integração ML", ex.getMessage(), request);
    }

    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(ParametroInvalidoException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Parâmetro inválido", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoDeParametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        var mensagem = "Parâmetro '" + ex.getName() + "' inválido: valor '" + ex.getValue() + "' não é compatível.";
        return response(HttpStatus.BAD_REQUEST, "Parâmetro inválido", mensagem, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var message = ex.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).orElse("Dados inválidos");
        return response(HttpStatus.BAD_REQUEST, "Requisição inválida", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> malformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "JSON inválido",
                "Remova quebras de linha dentro dos valores de titulo ou texto e envie um JSON válido.", request);
    }

    @ExceptionHandler(RegistroNotFoundException.class)
    public ResponseEntity<ErroResponse> notFound(RegistroNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Registro não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {} {}", request.getMethod(), request.getRequestURI(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado",
                "Não foi possível concluir a operação no momento. Tente novamente.", request);
    }

    private ResponseEntity<ErroResponse> response(HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErroResponse(Instant.now(), status.value(), error, message, request.getRequestURI()));
    }
}
