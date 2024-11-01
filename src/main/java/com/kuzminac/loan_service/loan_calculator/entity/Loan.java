package com.kuzminac.loan_service.loan_calculator.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_amount", nullable = false)
    private BigDecimal loanAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "number_of_payments", nullable = false)
    private Integer numberOfPayments;

    @Column(name = "total_payment", nullable = false)
    private BigDecimal totalPayment;

    @Column(name = "total_interest", nullable = false)
    private BigDecimal totalInterest;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
