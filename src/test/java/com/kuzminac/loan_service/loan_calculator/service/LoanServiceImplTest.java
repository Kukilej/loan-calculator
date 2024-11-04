// src/test/java/com/kuzminac/loan_service/loan_calculator/service/LoanServiceImplTest.java
package com.kuzminac.loan_service.loan_calculator.service;

import com.kuzminac.loan_service.loan_calculator.dto.LoanRequestDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanResponseDTO;
import com.kuzminac.loan_service.loan_calculator.dto.PaymentScheduleDTO;
import com.kuzminac.loan_service.loan_calculator.entity.Loan;
import com.kuzminac.loan_service.loan_calculator.repository.LoanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private LoanServiceImpl loanService;

    /**
     * Provides test data for successful loan calculations.
     * Each Arguments instance contains:
     * - LoanRequestDTO
     * - Expected total payment
     * - Expected total interest
     */
    private static Stream<Arguments> provideSuccessfulLoanCalculations() {
        return Stream.of(
                Arguments.of(
                        LoanRequestDTO.builder()
                                .loanAmount(new BigDecimal("27000.00"))
                                .interestRate(new BigDecimal("4.875"))
                                .numberOfPayments(36)
                                .build(),
                        new BigDecimal("29077.20"), // Expected total payment
                        new BigDecimal("2077.20")   // Expected total interest
                ),
                Arguments.of(
                        LoanRequestDTO.builder()
                                .loanAmount(new BigDecimal("0.01"))
                                .interestRate(new BigDecimal("2.5"))
                                .numberOfPayments(1)
                                .build(),
                        new BigDecimal("0.01"),
                        new BigDecimal("0.00")
                ),
                Arguments.of(
                        LoanRequestDTO.builder()
                                .loanAmount(new BigDecimal("10000.00"))
                                .interestRate(new BigDecimal("5.0"))
                                .numberOfPayments(12)
                                .build(),
                        new BigDecimal("10272.89"),
                        new BigDecimal("272.89")
                ),
                Arguments.of(
                        // Edge case: Zero interest rate
                        LoanRequestDTO.builder()
                                .loanAmount(new BigDecimal("12000.00"))
                                .interestRate(new BigDecimal("0.00"))
                                .numberOfPayments(24)
                                .build(),
                        new BigDecimal("12000.00"),
                        new BigDecimal("0.00")
                )
        );
    }



    @ParameterizedTest
    @MethodSource("provideSuccessfulLoanCalculations")
    @DisplayName("Test Successful Loan Calculations with Various Inputs")
    void calculateLoan_Success(LoanRequestDTO requestDTO, BigDecimal expectedTotalPayment, BigDecimal expectedTotalInterest) {

        // Arrange
        Loan savedLoan = Loan.builder()
                .id(1L)
                .loanAmount(requestDTO.getLoanAmount())
                .interestRate(requestDTO.getInterestRate())
                .numberOfPayments(requestDTO.getNumberOfPayments())
                .totalPayment(expectedTotalPayment)
                .totalInterest(expectedTotalInterest)
                .createdAt(LocalDateTime.now())
                .build();

        when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);

        // Act
        LoanResponseDTO responseDTO = loanService.calculateLoan(requestDTO);

        // Assert
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getLoanId()).isEqualTo(savedLoan.getId());
        assertThat(responseDTO.getLoanAmount()).isEqualByComparingTo(requestDTO.getLoanAmount());
        assertThat(responseDTO.getInterestRate()).isEqualByComparingTo(requestDTO.getInterestRate());
        assertThat(responseDTO.getNumberOfPayments()).isEqualTo(requestDTO.getNumberOfPayments());
        assertThat(responseDTO.getTotalPayment()).isCloseTo(expectedTotalPayment, within(new BigDecimal("0.1")));
        assertThat(responseDTO.getTotalInterest()).isCloseTo(expectedTotalInterest, within(new BigDecimal("0.1")));
        assertThat(responseDTO.getPaymentSchedule()).hasSize(requestDTO.getNumberOfPayments());

        // Verify each payment in the schedule
        List<PaymentScheduleDTO> schedule = responseDTO.getPaymentSchedule();
        BigDecimal balance = requestDTO.getLoanAmount();
        BigDecimal monthlyInterestRate = requestDTO.getInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        for (int i = 0; i < schedule.size(); i++) {
            PaymentScheduleDTO payment = schedule.get(i);
            BigDecimal expectedInterest = balance.multiply(monthlyInterestRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedPrincipal = payment.getPayment().subtract(expectedInterest).setScale(2, RoundingMode.HALF_UP);
            balance = balance.subtract(expectedPrincipal).setScale(2, RoundingMode.HALF_UP);

            if (i == schedule.size() - 1 && balance.compareTo(BigDecimal.ZERO) != 0) {
                // Adjust the last payment if there's any residual balance
                expectedPrincipal = expectedPrincipal.add(balance);
                balance = BigDecimal.ZERO;
            }

            assertThat(payment.getPeriod()).isEqualTo(i + 1);
            assertThat(payment.getPayment()).isCloseTo(requestDTO.getLoanAmount().compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : responseDTO.getTotalPayment().divide(BigDecimal.valueOf(requestDTO.getNumberOfPayments()), 2, RoundingMode.HALF_UP),
                    within(new BigDecimal("0.1")));
            assertThat(payment.getPrincipalAmount()).isCloseTo(expectedPrincipal, within(new BigDecimal("0.1")));
            assertThat(payment.getInterestAmount()).isCloseTo(expectedInterest, within(new BigDecimal("0.1")));
            assertThat(payment.getBalanceOwed()).isCloseTo(balance.max(BigDecimal.ZERO), within(new BigDecimal("0.1")));
        }

        // Capture the Loan entity being saved
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository, times(1)).save(loanCaptor.capture());
        Loan capturedLoan = loanCaptor.getValue();

        assertThat(capturedLoan.getLoanAmount()).isEqualByComparingTo(requestDTO.getLoanAmount());
        assertThat(capturedLoan.getInterestRate()).isEqualByComparingTo(requestDTO.getInterestRate());
        assertThat(capturedLoan.getNumberOfPayments()).isEqualTo(requestDTO.getNumberOfPayments());
        assertThat(capturedLoan.getTotalPayment()).isCloseTo(expectedTotalPayment, within(new BigDecimal("0.1")));
        assertThat(capturedLoan.getTotalInterest()).isCloseTo(expectedTotalInterest, within(new BigDecimal("0.1")));
        assertThat(capturedLoan.getCreatedAt()).isNotNull();
    }


    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {


        @Test
        @DisplayName("Test Single Payment Loan")
        void calculateLoan_SinglePayment() {
            // Arrange
            LoanRequestDTO requestDTO = LoanRequestDTO.builder()
                    .loanAmount(new BigDecimal("1000.00"))
                    .interestRate(new BigDecimal("5.0"))
                    .numberOfPayments(1)
                    .build();

            when(loanRepository.save(any(Loan.class))).thenReturn(
                    Loan.builder()
                            .id(5L)
                            .loanAmount(requestDTO.getLoanAmount())
                            .interestRate(requestDTO.getInterestRate())
                            .numberOfPayments(requestDTO.getNumberOfPayments())
                            .totalPayment(new BigDecimal("1000.00").add(new BigDecimal("4.17")))
                            .totalInterest(new BigDecimal("4.17"))
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            // Act
            LoanResponseDTO responseDTO = loanService.calculateLoan(requestDTO);

            // Assert
            assertThat(responseDTO).isNotNull();
            assertThat(responseDTO.getNumberOfPayments()).isEqualTo(1);
            assertThat(responseDTO.getTotalPayment()).isEqualByComparingTo(new BigDecimal("1004.17"));
            assertThat(responseDTO.getTotalInterest()).isEqualByComparingTo(new BigDecimal("4.17"));
            assertThat(responseDTO.getPaymentSchedule()).hasSize(1);

            PaymentScheduleDTO payment = responseDTO.getPaymentSchedule().get(0);
            assertThat(payment.getPeriod()).isEqualTo(1);
            assertThat(payment.getPayment()).isEqualByComparingTo(new BigDecimal("1004.17"));
            assertThat(payment.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(payment.getInterestAmount()).isEqualByComparingTo(new BigDecimal("4.17"));
            assertThat(payment.getBalanceOwed()).isEqualByComparingTo(BigDecimal.ZERO);

            // Verify that save was called once
            verify(loanRepository, times(1)).save(any(Loan.class));
        }
    }
}
