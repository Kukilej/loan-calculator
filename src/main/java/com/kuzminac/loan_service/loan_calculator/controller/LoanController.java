package com.kuzminac.loan_service.loan_calculator.controller;


import com.kuzminac.loan_service.loan_calculator.dto.LoanRequestDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanResponseDTO;
import com.kuzminac.loan_service.loan_calculator.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;

    @Autowired
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoanResponseDTO> calculateLoan(@Valid @RequestBody LoanRequestDTO requestDTO) {
        LoanResponseDTO responseDTO = loanService.calculateLoan(requestDTO);
        return ResponseEntity.ok(responseDTO);
    }
}
