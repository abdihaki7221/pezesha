package com.pezesha.cblms.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Entity model for loan repayments
 *
 * @author AOmar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("repayments")
public class Repayment {

    @Id
    private Long id;

    @Column("repayment_id")
    private String repaymentId;

    @Column("loan_id")
    private String loanId;

    @Column("amount")
    private BigDecimal amount;

    @Column("payment_date")
    private Instant paymentDate;

    @Column("principal_paid")
    private BigDecimal principalPaid;

    @Column("interest_paid")
    private BigDecimal interestPaid;

    @Column("allocation_strategy")
    private String allocationStrategy;

    @Column("payment_account_id")
    private String paymentAccountId;

    @Column("transaction_id")
    private String transactionId;

    @Column("created_at")
    private LocalDateTime createdAt;
}