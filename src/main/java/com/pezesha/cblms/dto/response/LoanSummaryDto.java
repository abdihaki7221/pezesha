package com.pezesha.cblms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author AOmar
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanSummaryDto {
    private String loanId;
    private String borrowerId;
    private BigDecimal principalAmount;
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingInterest;
    private String status;
    private Instant disbursementDate;
    private Instant maturityDate;
    private int daysOverdue;
}
