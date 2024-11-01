package com.kuzminac.loan_service.loan_calculator;

import org.springframework.boot.SpringApplication;

public class TestLoanCalculatorApplication {

	public static void main(String[] args) {
		SpringApplication.from(LoanCalculatorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
