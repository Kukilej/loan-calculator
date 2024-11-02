package com.kuzminac.loan_service.loan_calculator.controller;


import com.kuzminac.loan_service.loan_calculator.dto.LoanRequestDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanResponseDTO;
import com.kuzminac.loan_service.loan_calculator.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loans")
@Slf4j
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoanResponseDTO> calculateLoan(@Valid @RequestBody LoanRequestDTO requestDTO) {
        log.info("Received loan calculation request: {}", requestDTO);
        LoanResponseDTO responseDTO = loanService.calculateLoan(requestDTO);
        return ResponseEntity.ok(responseDTO);
    }
}
