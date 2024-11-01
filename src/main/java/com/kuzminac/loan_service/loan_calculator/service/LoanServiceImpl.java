package com.kuzminac.loan_service.loan_calculator.service;


import com.kuzminac.loan_service.loan_calculator.dto.LoanRequestDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanResponseDTO;
import com.kuzminac.loan_service.loan_calculator.dto.PaymentScheduleDTO;
import com.kuzminac.loan_service.loan_calculator.entity.Loan;
import com.kuzminac.loan_service.loan_calculator.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;

    @Override
    public LoanResponseDTO calculateLoan(LoanRequestDTO requestDTO) {

        // Extract loan details from request
        BigDecimal principal = requestDTO.getLoanAmount();
        BigDecimal annualInterestRate = requestDTO.getInterestRate();
        int numberOfPayments = requestDTO.getNumberOfPayments();

        // Calculate monthly interest rate
        BigDecimal monthlyInterestRate = calculateMonthlyInterestRate(annualInterestRate);

        // Calculate monthly payment using the loan formula
        BigDecimal payment = calculateMonthlyPayment(principal, monthlyInterestRate, numberOfPayments);

        // Calculate total payment and total interest
        BigDecimal totalPayment = payment.multiply(BigDecimal.valueOf(numberOfPayments));
        BigDecimal totalInterest = totalPayment.subtract(principal);

        // Generate payment schedule
        List<PaymentScheduleDTO> schedule = generatePaymentSchedule(principal, monthlyInterestRate, numberOfPayments, payment);

        // Persist loan details
        Loan savedLoan = loanRepository.save(createLoanEntity(principal, annualInterestRate, numberOfPayments, totalPayment, totalInterest));

        // Build and return response DTO
        return buildLoanResponse(savedLoan, principal, annualInterestRate, numberOfPayments, totalPayment, totalInterest, schedule);
    }


    /**
     * Calculates the monthly interest rate from the annual interest rate.
     *
     * @param annualRate Annual interest rate as a percentage (e.g., 4.875 for 4.875%)
     * @return Monthly interest rate as a decimal
     */
    private BigDecimal calculateMonthlyInterestRate(BigDecimal annualRate) {
        return annualRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the monthly payment using the loan formula.
     *
     * @param principal            Loan amount
     * @param monthlyInterestRate  Monthly interest rate
     * @param numberOfPayments     Total number of payments
     * @return Monthly payment amount
     */
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyInterestRate, int numberOfPayments) {
        BigDecimal numerator = monthlyInterestRate.multiply(principal);
        BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.add(monthlyInterestRate).pow(-numberOfPayments, new java.math.MathContext(10))
        );
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }


    /**
     * Generates the payment schedule for the loan.
     *
     * @param principal            Loan amount
     * @param monthlyInterestRate  Monthly interest rate
     * @param numberOfPayments     Total number of payments
     * @param payment              Monthly payment amount
     * @return List of payment schedule entries
     */
    private List<PaymentScheduleDTO> generatePaymentSchedule(BigDecimal principal, BigDecimal monthlyInterestRate, int numberOfPayments, BigDecimal payment) {
        List<PaymentScheduleDTO> schedule = new ArrayList<>();
        BigDecimal balance = principal;

        for (int period = 1; period <= numberOfPayments; period++) {
            BigDecimal interest = balance.multiply(monthlyInterestRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalAmount = payment.subtract(interest).setScale(2, RoundingMode.HALF_UP);
            balance = balance.subtract(principalAmount).setScale(2, RoundingMode.HALF_UP);

            // Adjust final payment if balance goes negative
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                principalAmount = principalAmount.add(balance);
                balance = BigDecimal.ZERO;
            }

            schedule.add(PaymentScheduleDTO.builder()
                    .period(period)
                    .payment(payment)
                    .principalAmount(principalAmount)
                    .interestAmount(interest)
                    .balanceOwed(balance)
                    .build());
        }

        return schedule;
    }

    /**
     * Creates a Loan entity from the provided details.
     *
     * @param principal        Loan amount
     * @param annualRate       Annual interest rate
     * @param payments         Number of payments
     * @param totalPayment     Total payment over the loan period
     * @param totalInterest    Total interest paid
     * @return Loan entity
     */
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

    /**
     * Builds the LoanResponseDTO from the saved loan and calculated details.
     *
     * @param loan              Saved Loan entity
     * @param principal         Loan amount
     * @param annualRate        Annual interest rate
     * @param payments          Number of payments
     * @param totalPayment      Total payment over the loan period
     * @param totalInterest     Total interest paid
     * @param schedule          Payment schedule
     * @return LoanResponseDTO
     */
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
