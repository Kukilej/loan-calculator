package com.kuzminac.loan_service.loan_calculator.service;


import com.kuzminac.loan_service.loan_calculator.dto.LoanRequestDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanResponseDTO;
import com.kuzminac.loan_service.loan_calculator.dto.PaymentScheduleDTO;
import com.kuzminac.loan_service.loan_calculator.entity.Loan;
import com.kuzminac.loan_service.loan_calculator.exception.LoanCalculationException;
import com.kuzminac.loan_service.loan_calculator.repository.LoanRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;

    // Increased precision to prevent rounding errors during calculations
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int SCALE = 10;

    @Override
    @Transactional
    public LoanResponseDTO calculateLoan(LoanRequestDTO requestDTO) {

        // Extract loan details from request
        BigDecimal principal = requestDTO.getLoanAmount();
        BigDecimal annualInterestRate = requestDTO.getInterestRate();
        int numberOfPayments = requestDTO.getNumberOfPayments();

        // Calculate monthly interest rate
        BigDecimal monthlyInterestRate = calculateMonthlyInterestRate(annualInterestRate);

        // Calculate monthly payment using the loan formula
        BigDecimal payment = calculateMonthlyPayment(principal, monthlyInterestRate, numberOfPayments);

        // Calculate total payment and total interest with high precision
        BigDecimal totalPayment = payment.multiply(BigDecimal.valueOf(numberOfPayments), MC);
        totalPayment = totalPayment.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalPayment.subtract(principal).setScale(2, RoundingMode.HALF_UP);
        log.debug("Total Payment: {}, Total Interest: {}", totalPayment, totalInterest);

        // Generate payment schedule
        List<PaymentScheduleDTO> schedule = generatePaymentSchedule(principal, monthlyInterestRate, numberOfPayments, payment);
        log.debug("Generated payment schedule with {} periods", schedule.size());

        // Persist loan details
        Loan savedLoan = loanRepository.save(createLoanEntity(principal, annualInterestRate, numberOfPayments, totalPayment, totalInterest));
        log.info("Loan calculation successful, saved loan ID: {}", savedLoan.getId());

        // Build and return response DTO
        return buildLoanResponse(savedLoan, principal, annualInterestRate, numberOfPayments, totalPayment, totalInterest, schedule);
    }

    private BigDecimal calculateMonthlyInterestRate(BigDecimal annualRate) {
        return annualRate
                .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), SCALE, RoundingMode.HALF_UP);
    }
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyInterestRate, int numberOfPayments) {

        // Handle the scenario where the interest rate is zero
        if (monthlyInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(numberOfPayments), 2, RoundingMode.HALF_UP);
        }

        try {
            BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyInterestRate, MC);
            BigDecimal onePlusRatePowN = onePlusRate.pow(numberOfPayments, MC);

            // Calculate the denominator: [1 - (1 + r)^-n] = [1 - 1 / (1 + r)^n]
            BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(onePlusRatePowN, SCALE, RoundingMode.HALF_UP), MC);

            // Calculate the numerator: r * principal
            BigDecimal numerator = monthlyInterestRate.multiply(principal, MC);


            // Compute the monthly payment: (r * principal) / denominator
            BigDecimal payment = numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);

            // Round the payment to two decimal places for actual payment
            return payment.setScale(2, RoundingMode.HALF_UP);

        } catch (ArithmeticException ae) {
            log.error("Arithmetic error during monthly payment calculation: {}", ae.getMessage(), ae);
            throw new LoanCalculationException("Error occurred while calculating the monthly payment.", ae);
        }
    }
    private List<PaymentScheduleDTO> generatePaymentSchedule(BigDecimal principal, BigDecimal monthlyInterestRate, int numberOfPayments, BigDecimal payment) {
        List<PaymentScheduleDTO> schedule = new ArrayList<>();
        BigDecimal balance = principal;

        for (int period = 1; period <= numberOfPayments; period++) {

            BigDecimal interest = balance.multiply(monthlyInterestRate, MC);
            interest = interest.setScale(10, RoundingMode.HALF_UP);
            BigDecimal principalAmount = payment.subtract(interest).setScale(10, RoundingMode.HALF_UP);
            balance = balance.subtract(principalAmount).setScale(10, RoundingMode.HALF_UP);

            // For the last payment, adjust for any residual due to rounding
            if (period == numberOfPayments && balance.compareTo(BigDecimal.ZERO) != 0) {
                principalAmount = principalAmount.add(balance);
                balance = BigDecimal.ZERO;
            }

            // Round for display purposes
            PaymentScheduleDTO paymentDTO = PaymentScheduleDTO.builder()
                    .period(period)
                    .payment(payment.setScale(2, RoundingMode.HALF_UP))
                    .principalAmount(principalAmount.setScale(2, RoundingMode.HALF_UP))
                    .interestAmount(interest.setScale(2, RoundingMode.HALF_UP))
                    .balanceOwed(balance.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))
                    .build();

            schedule.add(paymentDTO);
        }
        return schedule;
    }

    private Loan createLoanEntity(BigDecimal principal, BigDecimal annualRate, int payments, BigDecimal totalPayment, BigDecimal totalInterest) {
        return Loan.builder()
                .loanAmount(principal)
                .interestRate(annualRate)
                .numberOfPayments(payments)
                .totalPayment(totalPayment)
                .totalInterest(totalInterest)
                .createdAt(LocalDateTime.now())
                .build();
    }
    private LoanResponseDTO buildLoanResponse(Loan loan, BigDecimal principal, BigDecimal annualRate, int payments, BigDecimal totalPayment, BigDecimal totalInterest, List<PaymentScheduleDTO> schedule) {
        return LoanResponseDTO.builder()
                .loanId(loan.getId())
                .loanAmount(principal)
                .interestRate(annualRate)
                .numberOfPayments(payments)
                .totalPayment(totalPayment)
                .totalInterest(totalInterest)
                .paymentSchedule(schedule)
                .build();
    }
}
