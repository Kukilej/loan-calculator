package com.kuzminac.loan_service.loan_calculator.exception;

import org.springframework.validation.BindingResult;

public class InvalidLoanParametersException extends RuntimeException {
    private final BindingResult bindingResult;

    public InvalidLoanParametersException(String message, BindingResult bindingResult) {
        super(message);
        this.bindingResult = bindingResult;
    }

    public BindingResult getBindingResult() {
        return bindingResult;
    }
}
