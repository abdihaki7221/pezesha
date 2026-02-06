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
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanRepaymentResponse {

    private String repaymentId;

    private String loanId;

    private BigDecimal amount;

    private Instant paymentDate;

    private Allocation allocation;

    private LoanBalance loanBalance;


    private String status;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Allocation {
        private BigDecimal principal;
        private BigDecimal interest;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanBalance {
        private BigDecimal outstandingPrincipal;
        private BigDecimal outstandingInterest;
        private BigDecimal totalOutstanding;
    }
}