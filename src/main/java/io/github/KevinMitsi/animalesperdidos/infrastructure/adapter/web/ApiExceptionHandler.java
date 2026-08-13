package io.github.kevinmitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.kevinmitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.CompletionException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({BusinessRuleViolation.class, IllegalArgumentException.class})
    ProblemDetail businessRule(RuntimeException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        detail.setTitle("Business rule violation");
        return detail;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    ProblemDetail validation(WebExchangeBindException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request contains invalid fields");
        detail.setProperty("errors", exception.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList());
        return detail;
    }

    @ExceptionHandler(CompletionException.class)
    ProblemDetail asyncFailure(CompletionException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof BusinessRuleViolation violation) {
            return businessRule(violation);
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "The operation could not be completed");
    }
}
