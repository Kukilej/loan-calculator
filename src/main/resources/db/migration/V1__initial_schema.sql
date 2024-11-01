CREATE TABLE loans (
    id SERIAL PRIMARY KEY,
    loan_amount NUMERIC(15, 2) NOT NULL,
    interest_rate NUMERIC(5, 4) NOT NULL,
    number_of_payments INTEGER NOT NULL,
    total_payment NUMERIC(15, 2) NOT NULL,
    total_interest NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'UTC')
);
