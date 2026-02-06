package com.pezesha.cblms.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author AOmar
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("tb_loans")
public class Loan extends BaseEntity {

    @Id
    private Long id;

    private String loanId;

    private String borrowerId;

    private BigDecimal principalAmount;

    private BigDecimal interestRate;

    private BigDecimal originationFee;

    private String currency;

    private Instant disbursementDate;

    private Instant maturityDate;

    private String status;

    private BigDecimal outstandingPrincipal;

    private BigDecimal outstandingInterest;

    private BigDecimal totalPaid = BigDecimal.ZERO;

    private Instant lastPaymentDate;

    private Instant closureDate;
}