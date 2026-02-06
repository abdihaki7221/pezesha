package com.pezesha.cblms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for loan list with pagination
 *
 * @author AOmar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanListResponse {

    private List<LoanSummary> loans;
    private Pagination pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanSummary {
        private String loanId;
        private String borrowerId;
        private BigDecimal principalAmount;
        private BigDecimal outstandingPrincipal;
        private BigDecimal outstandingInterest;
        private String status;
        private Instant disbursementDate;
        private Instant maturityDate;
        private Long daysOverdue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private Integer currentPage;
        private Integer pageSize;
        private Long totalPages;
        private Long totalElements;
    }
}