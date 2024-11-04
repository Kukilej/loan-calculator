
package com.kuzminac.loan_service.loan_calculator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuzminac.loan_service.loan_calculator.dto.LoanRequestDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanResponseDTO;
import com.kuzminac.loan_service.loan_calculator.dto.PaymentScheduleDTO;
import com.kuzminac.loan_service.loan_calculator.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String ENDPOINT = "/api/v1/loans/calculate";


    @MockBean
    private LoanService loanService;



    @Test
    @DisplayName("Controller Test: Successful Loan Calculation")
    void calculateLoan_Success() throws Exception {
        // Arrange
        LoanRequestDTO requestDTO = LoanRequestDTO.builder()
                .loanAmount(new BigDecimal("1000.00"))
                .interestRate(new BigDecimal("5.0"))
                .numberOfPayments(3)
                .build();

        List<PaymentScheduleDTO> paymentSchedule = List.of(
                PaymentScheduleDTO.builder()
                        .period(1)
                        .payment(new BigDecimal("341.67"))
                        .principalAmount(new BigDecimal("333.33"))
                        .interestAmount(new BigDecimal("8.34"))
                        .balanceOwed(new BigDecimal("666.67"))
                        .build(),
                PaymentScheduleDTO.builder()
                        .period(2)
                        .payment(new BigDecimal("341.67"))
                        .principalAmount(new BigDecimal("333.33"))
                        .interestAmount(new BigDecimal("8.34"))
                        .balanceOwed(new BigDecimal("333.34"))
                        .build(),
                PaymentScheduleDTO.builder()
                        .period(3)
                        .payment(new BigDecimal("341.66"))
                        .principalAmount(new BigDecimal("333.32"))
                        .interestAmount(new BigDecimal("8.34"))
                        .balanceOwed(new BigDecimal("0.00"))
                        .build()
        );

        LoanResponseDTO responseDTO = LoanResponseDTO.builder()
                .loanId(1L)
                .loanAmount(requestDTO.getLoanAmount())
                .interestRate(requestDTO.getInterestRate())
                .numberOfPayments(requestDTO.getNumberOfPayments())
                .totalPayment(new BigDecimal("1025.00"))
                .totalInterest(new BigDecimal("25.00"))
                .paymentSchedule(paymentSchedule)
                .build();

        Mockito.when(loanService.calculateLoan(any(LoanRequestDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(1L))
                .andExpect(jsonPath("$.loanAmount").value(1000.00))
                .andExpect(jsonPath("$.interestRate").value(5.0))
                .andExpect(jsonPath("$.numberOfPayments").value(3))

                .andExpect(jsonPath("$.totalPayment").value(1025.00))
                .andExpect(jsonPath("$.totalInterest").value(25.00))
                .andExpect(jsonPath("$.paymentSchedule").isArray())
                .andExpect(jsonPath("$.paymentSchedule.length()").value(3))
                .andExpect(jsonPath("$.paymentSchedule[0].period").value(1))
                .andExpect(jsonPath("$.paymentSchedule[0].payment").value(341.67))
                .andExpect(jsonPath("$.paymentSchedule[0].principalAmount").value(333.33))
                .andExpect(jsonPath("$.paymentSchedule[0].interestAmount").value(8.34))
                .andExpect(jsonPath("$.paymentSchedule[0].balanceOwed").value(666.67))
                .andExpect(jsonPath("$.paymentSchedule[2].balanceOwed").value(0.00));
    }

    @Test
    @DisplayName("Controller Test: Validation Failure")
    void calculateLoan_ValidationFailure() throws Exception {
        // Arrange
        LoanRequestDTO requestDTO = LoanRequestDTO.builder()
                .loanAmount(new BigDecimal("-1000.00")) // Invalid amount
                .interestRate(new BigDecimal("5.0"))
                .numberOfPayments(0) // Invalid number of payments
                .build();

        // Act & Assert
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("Invalid input parameters"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details").value(org.hamcrest.Matchers.hasItems(
                        "Field 'loanAmount' Loan amount must be greater than 0",
                        "Field 'numberOfPayments' Number of payments must be at least 1"
                )));
    }

    @Test
    @DisplayName("Controller Test: Loan Calculation Exception")
    void calculateLoan_Exception() throws Exception {
        // Arrange
        LoanRequestDTO requestDTO = LoanRequestDTO.builder()
                .loanAmount(new BigDecimal("1000.00"))
                .interestRate(new BigDecimal("5.0"))
                .numberOfPayments(3)
                .build();

        Mockito.when(loanService.calculateLoan(any(LoanRequestDTO.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}
