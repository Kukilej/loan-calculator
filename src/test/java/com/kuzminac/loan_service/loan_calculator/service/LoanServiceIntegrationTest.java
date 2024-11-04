
package com.kuzminac.loan_service.loan_calculator.service;

import com.kuzminac.loan_service.loan_calculator.dto.LoanRequestDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanResponseDTO;
import com.kuzminac.loan_service.loan_calculator.entity.Loan;
import com.kuzminac.loan_service.loan_calculator.repository.LoanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Testcontainers
@SpringBootTest
class LoanServiceIntegrationTest {

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15.3")
            .withDatabaseName("postgres")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.flyway.user", postgreSQLContainer::getUsername);
        registry.add("spring.flyway.password", postgreSQLContainer::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private LoanServiceImpl loanService;

    @Autowired
    private LoanRepository loanRepository;

    private static Stream<Arguments> provideSuccessfulLoanCalculations() {
        return Stream.of(
                Arguments.of(
                        LoanRequestDTO.builder()
                                .loanAmount(new BigDecimal("27000.00"))
                                .interestRate(new BigDecimal("4.875"))
                                .numberOfPayments(36)
                                .build(),
                        new BigDecimal("29077.18"),
                        new BigDecimal("2077.18")
                ),
                Arguments.of(
                        LoanRequestDTO.builder()
                                .loanAmount(new BigDecimal("10000.00"))
                                .interestRate(new BigDecimal("5.0"))
                                .numberOfPayments(12)
                                .build(),
                        new BigDecimal("10272.84"),
                        new BigDecimal("272.84")
                )
        );
    }

    @ParameterizedTest
    @MethodSource("provideSuccessfulLoanCalculations")
    @DisplayName("Integration Test: Successful Loan Calculations with Various Inputs")
    void calculateLoan_Success(LoanRequestDTO requestDTO,
                               BigDecimal expectedTotalPayment,
                               BigDecimal expectedTotalInterest) {
        // Act
        LoanResponseDTO responseDTO = loanService.calculateLoan(requestDTO);

        // Assert
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getLoanId()).isNotNull();
        assertThat(responseDTO.getLoanAmount()).isEqualByComparingTo(requestDTO.getLoanAmount());
        assertThat(responseDTO.getInterestRate()).isEqualByComparingTo(requestDTO.getInterestRate());
        assertThat(responseDTO.getNumberOfPayments()).isEqualTo(requestDTO.getNumberOfPayments());

        assertThat(responseDTO.getTotalPayment()).isCloseTo(expectedTotalPayment, within(new BigDecimal("0.1")));
        assertThat(responseDTO.getTotalInterest()).isCloseTo(expectedTotalInterest, within(new BigDecimal("0.1")));

        assertThat(responseDTO.getPaymentSchedule()).hasSize(requestDTO.getNumberOfPayments());

        // Verify persistence
        Optional<Loan> optionalLoan = loanRepository.findById(responseDTO.getLoanId());
        assertThat(optionalLoan).isPresent();
        Loan savedLoan = optionalLoan.get();
        assertThat(savedLoan.getLoanAmount()).isEqualByComparingTo(requestDTO.getLoanAmount());
        assertThat(savedLoan.getInterestRate()).isEqualByComparingTo(requestDTO.getInterestRate());
        assertThat(savedLoan.getNumberOfPayments()).isEqualTo(requestDTO.getNumberOfPayments());
        assertThat(savedLoan.getTotalPayment()).isCloseTo(expectedTotalPayment, within(new BigDecimal("0.1")));
        assertThat(savedLoan.getTotalInterest()).isCloseTo(expectedTotalInterest, within(new BigDecimal("0.1")));
    }

    @Test
    @DisplayName("Integration Test: Verify Payment Schedule Persistence")
    void verifyPaymentSchedulePersistence() {
        // Arrange
        LoanRequestDTO requestDTO = LoanRequestDTO.builder()
                .loanAmount(new BigDecimal("5000.00"))
                .interestRate(new BigDecimal("5.0"))
                .numberOfPayments(12)
                .build();

        // Act
        LoanResponseDTO responseDTO = loanService.calculateLoan(requestDTO);

        // Assert
        assertThat(responseDTO.getPaymentSchedule()).hasSize(12);

        // Verify that the loan is persisted correctly
        Optional<Loan> optionalLoan = loanRepository.findById(responseDTO.getLoanId());
        assertThat(optionalLoan).isPresent();
        Loan savedLoan = optionalLoan.get();
        assertThat(savedLoan.getCreatedAt()).isNotNull();
    }
}
