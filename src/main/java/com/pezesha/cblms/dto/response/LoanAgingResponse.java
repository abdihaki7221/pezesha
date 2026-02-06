package com.pezesha.cblms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @author AOmar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAgingResponse {

    @JsonProperty("reportDate")
    private Instant reportDate;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("summary")
    private AgingSummary summary;

    @JsonProperty("loans")
    private List<AgingLoan> loans;

    @JsonProperty("totals")
    private AgingTotals totals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingSummary {
        @JsonProperty("current")
        private AgingBucket current;

        @JsonProperty("days1to30")
        private AgingBucket days1to30;

        @JsonProperty("days31to60")
        private AgingBucket days31to60;

        @JsonProperty("days61to90")
        private AgingBucket days61to90;

        @JsonProperty("over90Days")
        private AgingBucket over90Days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingBucket {
        @JsonProperty("count")
        private Integer count;

        @JsonProperty("totalOutstanding")
        private BigDecimal totalOutstanding;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingLoan {
        @JsonProperty("loanId")
        private String loanId;

        @JsonProperty("borrowerId")
        private String borrowerId;

        @JsonProperty("principalAmount")
        private BigDecimal principalAmount;

        @JsonProperty("outstandingPrincipal")
        private BigDecimal outstandingPrincipal;

        @JsonProperty("outstandingInterest")
        private BigDecimal outstandingInterest;

        @JsonProperty("totalOutstanding")
        private BigDecimal totalOutstanding;

        @JsonProperty("disbursementDate")
        private Instant disbursementDate;

        @JsonProperty("maturityDate")
        private Instant maturityDate;

        @JsonProperty("daysOverdue")
        private Long daysOverdue;

        @JsonProperty("agingCategory")
        private String agingCategory;

        @JsonProperty("status")
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingTotals {
        @JsonProperty("totalLoans")
        private Integer totalLoans;

        @JsonProperty("totalOutstanding")
        private BigDecimal totalOutstanding;

        @JsonProperty("totalOverdue")
        private BigDecimal totalOverdue;
    }
}
