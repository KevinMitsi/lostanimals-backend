package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.application.exception.BusinessRuleViolation;
import io.github.KevinMitsi.animalesperdidos.application.exception.BotVerificationFailed;
import io.github.KevinMitsi.animalesperdidos.application.exception.DuplicateUserData;
import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidCredentials;
import io.github.KevinMitsi.animalesperdidos.application.exception.InvalidOrExpiredToken;
import io.github.KevinMitsi.animalesperdidos.application.exception.EmailNotVerified;
import io.github.KevinMitsi.animalesperdidos.application.exception.ResourceNotFound;
import io.github.KevinMitsi.animalesperdidos.application.exception.ForbiddenOperation;
import io.github.KevinMitsi.animalesperdidos.application.exception.ConcurrentUpdate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.CompletionException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DuplicateUserData.class)
    ProblemDetail duplicate(DuplicateUserData exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentials.class)
    ProblemDetail credentials(InvalidCredentials exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(EmailNotVerified.class)
    ProblemDetail emailNotVerified(EmailNotVerified exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(InvalidOrExpiredToken.class)
    ProblemDetail invalidToken(InvalidOrExpiredToken exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(BotVerificationFailed.class)
    ProblemDetail bot(BotVerificationFailed exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler({BusinessRuleViolation.class, IllegalArgumentException.class, IllegalStateException.class})
    ProblemDetail businessRule(RuntimeException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        detail.setTitle("Business rule violation");
        return detail;
    }

    @ExceptionHandler(ResourceNotFound.class)
    ProblemDetail notFound(ResourceNotFound exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenOperation.class)
    ProblemDetail forbidden(ForbiddenOperation exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(ConcurrentUpdate.class)
    ProblemDetail conflict(ConcurrentUpdate exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
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
        if (cause instanceof DuplicateUserData duplicate) return duplicate(duplicate);
        if (cause instanceof InvalidCredentials credentials) return credentials(credentials);
        if (cause instanceof BotVerificationFailed bot) return bot(bot);
        if (cause instanceof EmailNotVerified email) return emailNotVerified(email);
        if (cause instanceof InvalidOrExpiredToken token) return invalidToken(token);
        if (cause instanceof ResourceNotFound missing) return notFound(missing);
        if (cause instanceof ForbiddenOperation forbidden) return forbidden(forbidden);
        if (cause instanceof ConcurrentUpdate conflict) return conflict(conflict);
        if (cause instanceof IllegalArgumentException invalid) return businessRule(invalid);
        if (cause instanceof IllegalStateException invalidState) return businessRule(invalidState);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "The operation could not be completed");
    }
}
