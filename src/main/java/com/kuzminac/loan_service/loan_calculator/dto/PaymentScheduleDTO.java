package com.kuzminac.loan_service.loan_calculator.dto;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentScheduleDTO {
    private Integer period;
    private BigDecimal payment;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal balanceOwed;
}
