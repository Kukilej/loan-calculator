package com.kuzminac.loan_service.loan_calculator.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LoanRequestDTO {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "0.01", message = "Loan amount must be greater than 0")
    private BigDecimal loanAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate must be at least 0")
    private BigDecimal interestRate;

    @NotNull(message = "Number of payments is required")
    @Min(value = 1, message = "Number of payments must be at least 1")
    private Integer numberOfPayments;
}
