package com.kuzminac.loan_service.loan_calculator.exception;

public class LoanCalculationException extends RuntimeException {
    public LoanCalculationException(String message) {
        super(message);
    }

    public LoanCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
