package com.kuzminac.loan_service.loan_calculator.exception;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.kuzminac.loan_service.loan_calculator.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(LoanCalculationException.class)
    public ResponseEntity<ErrorResponseDTO> handleLoanCalculationException(LoanCalculationException ex, HttpServletRequest request) {
        log.error("LoanCalculationException: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Loan Calculation Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        log.warn("Validation failed for request to {}: {}", request.getRequestURI(), details);
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input parameters")
                .path(request.getRequestURI())
                .details(details)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Malformed JSON request";

        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .findFirst()
                    .orElse("Unknown");
            String invalidValue = ife.getValue() != null ? ife.getValue().toString() : "null";
            String targetType = ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : "Unknown";
            message = String.format("Invalid value '%s' for field '%s'. Expected type: %s.",
                    invalidValue, fieldName, targetType);
        } else if (cause instanceof JsonParseException) {
            JsonParseException jpe = (JsonParseException) cause;
            message = "JSON parse error: " + jpe.getOriginalMessage();
        }

        log.warn("Malformed JSON request to {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Malformed JSON Request")
                .message(message)
                .path(request.getRequestURI())
                .details(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }



    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String formatFieldError(FieldError fieldError) {
        return String.format("Field '%s' %s", fieldError.getField(), fieldError.getDefaultMessage());
    }
}
