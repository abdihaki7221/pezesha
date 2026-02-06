CREATE TABLE IF NOT EXISTS tb_accounts
(
    id                BIGINT PRIMARY KEY DEFAULT (floor(random()*9000000000 + 1000000000)::bigint),
    code              VARCHAR(100) NOT NULL UNIQUE,
    name              VARCHAR(255) NOT NULL,
    type              VARCHAR(50) NOT NULL,
    currency          VARCHAR(3) NOT NULL,
    parent_account_id BIGINT,
    current_balance   NUMERIC(20,4) DEFAULT 0.0000,
    is_active         BOOLEAN DEFAULT FALSE,
    has_transaction   BOOLEAN DEFAULT FALSE,
    is_deleted        BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMP,
    created_by        VARCHAR(50),
    updated_at        TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_transactions (
                                               id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                               transaction_id VARCHAR(255) NOT NULL UNIQUE,
                                               idempotency_key VARCHAR(255) NOT NULL,
                                               status VARCHAR(50) NOT NULL,
                                               transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                               description VARCHAR(500) NOT NULL,
                                               currency VARCHAR(3) NOT NULL,
                                               total_debit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                                               total_credit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                                               metadata JSONB,
                                               posted_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                               reversed_by VARCHAR(255),
                                               reversal_transaction_id BIGINT,
                                               reversal_date TIMESTAMP WITH TIME ZONE,
                                               reason VARCHAR(500),
                                               created_by VARCHAR(255),
                                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                               updated_at TIMESTAMP WITH TIME ZONE,
                                               FOREIGN KEY (reversal_transaction_id) REFERENCES tb_transactions(id)
);

CREATE TABLE IF NOT EXISTS tb_journal_entries (
                                                  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                                  transaction_id BIGINT NOT NULL,
                                                  account_id BIGINT NOT NULL,
                                                  account_code VARCHAR(50) NOT NULL,
                                                  account_name VARCHAR(100) NOT NULL,
                                                  account_type VARCHAR(50) NOT NULL,
                                                  debit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                                                  credit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                                                  transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                                  FOREIGN KEY (transaction_id) REFERENCES tb_transactions(id),
                                                  FOREIGN KEY (account_id) REFERENCES tb_accounts(id)
);

CREATE TABLE IF NOT EXISTS tb_idempotency (
                                              id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                              key VARCHAR(255) NOT NULL UNIQUE,
                                              transaction_id BIGINT NOT NULL,
                                              created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                              FOREIGN KEY (transaction_id) REFERENCES tb_transactions(id)
);

CREATE TABLE IF NOT EXISTS tb_borrowers (
                                            id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                            borrower_id VARCHAR(255) NOT NULL UNIQUE,
                                            name VARCHAR(255) NOT NULL,
                                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP WITH TIME ZONE,
                                            is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS tb_loans (
                                        id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                        loan_id VARCHAR(255) NOT NULL UNIQUE,
                                        borrower_id VARCHAR(255) NOT NULL,
                                        principal_amount NUMERIC(20,4) NOT NULL,
                                        interest_rate NUMERIC(20,4) NOT NULL,
                                        origination_fee NUMERIC(20,4) DEFAULT 0.0000,
                                        currency VARCHAR(3) NOT NULL,
                                        disbursement_date TIMESTAMP WITH TIME ZONE,
                                        maturity_date TIMESTAMP WITH TIME ZONE,
                                        status VARCHAR(50) NOT NULL,
                                        outstanding_principal NUMERIC(20,4) DEFAULT 0.0000,
                                        outstanding_interest NUMERIC(20,4) DEFAULT 0.0000,
                                        total_paid NUMERIC(20,4) DEFAULT 0.0000,
                                        last_payment_date TIMESTAMP WITH TIME ZONE,
                                        closure_date TIMESTAMP WITH TIME ZONE,
                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP WITH TIME ZONE,
                                        created_by VARCHAR(255),
                                        updated_by VARCHAR(255),
                                        is_deleted BOOLEAN DEFAULT FALSE,
                                        FOREIGN KEY (borrower_id) REFERENCES tb_borrowers(borrower_id)
);

CREATE TABLE IF NOT EXISTS repayments (
                                          id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                          repayment_id VARCHAR(255) NOT NULL UNIQUE,
                                          loan_id VARCHAR(255) NOT NULL,
                                          amount NUMERIC(20,4) NOT NULL,
                                          payment_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                          principal_paid NUMERIC(20,4) DEFAULT 0.0000,
                                          interest_paid NUMERIC(20,4) DEFAULT 0.0000,
                                          allocation_strategy VARCHAR(100),
                                          payment_account_id VARCHAR(100),
                                          transaction_id VARCHAR(255),
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP,
                                          is_deleted BOOLEAN DEFAULT FALSE,
                                          FOREIGN KEY (loan_id) REFERENCES tb_loans(loan_id),
                                          FOREIGN KEY (payment_account_id) REFERENCES tb_accounts(code),
                                          FOREIGN KEY (transaction_id) REFERENCES tb_transactions(transaction_id)
);

CREATE TABLE IF NOT EXISTS write_offs (
                                          id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                          write_off_id VARCHAR(255) NOT NULL UNIQUE,
                                          loan_id VARCHAR(255) NOT NULL,
                                          write_off_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                          write_off_type VARCHAR(100) NOT NULL,
                                          principal_written_off NUMERIC(20,4) DEFAULT 0.0000,
                                          interest_written_off NUMERIC(20,4) DEFAULT 0.0000,
                                          total_written_off NUMERIC(20,4) DEFAULT 0.0000,
                                          reason VARCHAR(500),
                                          transaction_id VARCHAR(255),
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP,
                                          is_deleted BOOLEAN DEFAULT FALSE,
                                          FOREIGN KEY (loan_id) REFERENCES tb_loans(loan_id),
                                          FOREIGN KEY (transaction_id) REFERENCES tb_transactions(transaction_id)
);