package com.kuzminac.loan_service.loan_calculator.service;

import com.kuzminac.loan_service.loan_calculator.dto.LoanRequestDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanResponseDTO;

public interface LoanService {
    LoanResponseDTO calculateLoan(LoanRequestDTO requestDTO);
}