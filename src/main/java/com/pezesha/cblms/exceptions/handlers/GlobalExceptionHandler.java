package com.pezesha.cblms.exceptions.handlers;

import com.pezesha.cblms.dto.response.ResponseDto;
import com.pezesha.cblms.exceptions.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * @author AOmar
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Component
public class GlobalExceptionHandler {
    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ResponseDto<Object>>> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        ResponseDto<Object> response = new ResponseDto<>();
        response.setStatus("BAD_REQUEST");
        response.setMessage(message);
        response.setStatusCode("400");

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }


    @ExceptionHandler(ServiceValidation.class)
    public ResponseEntity<Mono<ResponseDto<Object>>> handleResponseStatusException(
            ServiceValidation ex) {
        ResponseDto<Object> errorMessage =
                ResponseDto.builder()
                        .status(String.valueOf(BAD_REQUEST))
                        .statusCode(String.valueOf(BAD_REQUEST.value()))
                        .message(ex.getMessage())
                        .build();

        return ResponseEntity.status(BAD_REQUEST).body(Mono.just(errorMessage));
    }

    @ExceptionHandler(ExcessiveDateRangeException.class)
    public ResponseEntity<Mono<ResponseDto<Object>>> handleExcessiveDateRangeException(
            ExcessiveDateRangeException ex) {
        ResponseDto<Object> errorMessage =
                ResponseDto.builder()
                        .status(String.valueOf(BAD_REQUEST))
                        .statusCode(String.valueOf(BAD_REQUEST.value()))
                        .message(ex.getMessage())
                        .build();

        return ResponseEntity.status(BAD_REQUEST).body(Mono.just(errorMessage));
    }

    @ExceptionHandler(InvalidDateException.class)
    public ResponseEntity<Mono<ResponseDto<Object>>> handleInvalidDateException(
            InvalidDateException ex) {
        ResponseDto<Object> errorMessage =
                ResponseDto.builder()
                        .status(String.valueOf(BAD_REQUEST))
                        .statusCode(String.valueOf(BAD_REQUEST.value()))
                        .message(ex.getMessage())
                        .build();

        return ResponseEntity.status(BAD_REQUEST).body(Mono.just(errorMessage));
    }

    @ExceptionHandler(UnbalancedBalanceSheetException.class)
    public ResponseEntity<Mono<ResponseDto<Object>>> handleUnbalancedBalanceSheetException(
            UnbalancedBalanceSheetException ex) {
        ResponseDto<Object> errorMessage =
                ResponseDto.builder()
                        .status(String.valueOf(BAD_REQUEST))
                        .statusCode(String.valueOf(BAD_REQUEST.value()))
                        .message(ex.getMessage())
                        .build();

        return ResponseEntity.status(BAD_REQUEST).body(Mono.just(errorMessage));
    }
    @ExceptionHandler(UnbalancedTrialBalanceException.class)
    public ResponseEntity<Mono<ResponseDto<Object>>> handleUnbalancedTrialBalanceException(
            UnbalancedTrialBalanceException ex) {
        ResponseDto<Object> errorMessage =
                ResponseDto.builder()
                        .status(String.valueOf(BAD_REQUEST))
                        .statusCode(String.valueOf(BAD_REQUEST.value()))
                        .message(ex.getMessage())
                        .build();

        return ResponseEntity.status(BAD_REQUEST).body(Mono.just(errorMessage));
    }
}
