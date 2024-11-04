package com.kuzminac.loan_service.loan_calculator.controller;


import com.kuzminac.loan_service.loan_calculator.dto.ErrorResponseDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanRequestDTO;
import com.kuzminac.loan_service.loan_calculator.dto.LoanResponseDTO;
import com.kuzminac.loan_service.loan_calculator.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
@Slf4j
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;


    @Operation(summary = "Calculate Loan Details",
            description = "Calculates loan details including payment schedule based on the provided loan amount, interest rate, and number of payments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully calculated loan details",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoanResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/calculate")
    public ResponseEntity<LoanResponseDTO> calculateLoan(@Valid @RequestBody LoanRequestDTO requestDTO) {
        log.info("Received loan calculation request: {}", requestDTO);
        LoanResponseDTO responseDTO = loanService.calculateLoan(requestDTO);
        return ResponseEntity.ok(responseDTO);
    }
}
