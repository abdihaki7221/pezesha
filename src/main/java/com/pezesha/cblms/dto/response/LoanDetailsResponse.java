package com.pezesha.cblms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for loan details
 *
 * @author AOmar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDetailsResponse {

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
    private BigDecimal totalOutstanding;
    private BigDecimal totalPaid;
    private Long daysOverdue;
    private Instant lastPaymentDate;
    private List<RepaymentHistoryItem> repaymentHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepaymentHistoryItem {
        private String repaymentId;
        private BigDecimal amount;
        private Instant date;
        private BigDecimal principalPaid;
        private BigDecimal interestPaid;
    }
}