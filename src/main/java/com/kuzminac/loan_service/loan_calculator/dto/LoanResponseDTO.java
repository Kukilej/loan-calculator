package com.kuzminac.loan_service.loan_calculator.dto;


import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponseDTO {
    private Long loanId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer numberOfPayments;
    private BigDecimal totalPayment;
    private BigDecimal totalInterest;
    private List<PaymentScheduleDTO> paymentSchedule;
}
