package com.phorest.techtest.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.phorest.techtest.controller.FruitMachineController;
import com.phorest.techtest.web.ConfigureMachineRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        this.logAppender = new ListAppender<>();
        this.logAppender.start();
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).addAppender(this.logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).detachAppender(this.logAppender);
    }

    @Test
    void handleIllegalArgumentCapturesTheExceptionMessageAsTheDetail() {
        ProblemDetail problemDetail = handler.handleIllegalArgument(
                new IllegalArgumentException("smallPrizeRunLength cannot exceed slotCount"));

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getDetail()).isEqualTo("smallPrizeRunLength cannot exceed slotCount");
        assertLogged(Level.WARN, "smallPrizeRunLength cannot exceed slotCount");
    }

    @Test
    void handleValidationCapturesTheFieldErrorAsTheDetail() throws NoSuchMethodException {
        MethodArgumentNotValidException exception = methodArgumentNotValidException(
                new FieldError("request", "initialBalance", "must be greater than or equal to 0"));

        ProblemDetail problemDetail = handler.handleValidation(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getDetail()).isEqualTo("initialBalance: must be greater than or equal to 0");
        assertLogged(Level.WARN, "initialBalance: must be greater than or equal to 0");
    }

    @Test
    void handleValidationJoinsMultipleFieldErrorsIntoOneDetail() throws NoSuchMethodException {
        MethodArgumentNotValidException exception = methodArgumentNotValidException(
                new FieldError("request", "initialBalance", "must be greater than or equal to 0"),
                new FieldError("request", "costPerPlay", "must be greater than 0"));

        ProblemDetail problemDetail = handler.handleValidation(exception);

        assertThat(problemDetail.getDetail())
                .isEqualTo("initialBalance: must be greater than or equal to 0; costPerPlay: must be greater than 0");
    }

    @Test
    void handleUnexpectedReturnsAGenericDetailAndNeverLeaksTheRealExceptionMessage() {
        ProblemDetail problemDetail = handler.handleUnexpected(new RuntimeException("db connection string: secret"));

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).isEqualTo("An unexpected error occurred");

        // the real exception is still captured server-side, at ERROR since it's unexpected
        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("db connection string: secret");
                });
    }

    private void assertLogged(Level level, String message) {
        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(level);
                    assertThat(event.getFormattedMessage()).contains(message);
                });
    }

    private MethodArgumentNotValidException methodArgumentNotValidException(FieldError... fieldErrors)
            throws NoSuchMethodException {
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        for (FieldError fieldError : fieldErrors) {
            bindingResult.addError(fieldError);
        }

        MethodParameter parameter = new MethodParameter(
                FruitMachineController.class.getMethod("configure", ConfigureMachineRequestDto.class), 0);
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }
}
