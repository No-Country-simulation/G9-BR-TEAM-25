package br.com.techmind.classificador.exception;

import br.com.techmind.classificador.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MlIntegrationException.class)
    public ResponseEntity<ErroResponse> ml(MlIntegrationException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_GATEWAY, "Erro de integração ML", ex.getMessage(), request);
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

    private ResponseEntity<ErroResponse> response(HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErroResponse(Instant.now(), status.value(), error, message, request.getRequestURI()));
    }
}
