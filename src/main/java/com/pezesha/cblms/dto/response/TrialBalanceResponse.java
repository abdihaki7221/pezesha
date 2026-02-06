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
public class TrialBalanceResponse {

    @JsonProperty("reportDate")
    private Instant reportDate;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("accounts")
    private List<TrialBalanceAccount> accounts;

    @JsonProperty("totals")
    private TrialBalanceTotals totals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrialBalanceAccount {
        @JsonProperty("accountId")
        private String accountId;

        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;

        @JsonProperty("debit")
        private BigDecimal debit;

        @JsonProperty("credit")
        private BigDecimal credit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrialBalanceTotals {
        @JsonProperty("totalDebit")
        private BigDecimal totalDebit;

        @JsonProperty("totalCredit")
        private BigDecimal totalCredit;

        @JsonProperty("difference")
        private BigDecimal difference;

        @JsonProperty("isBalanced")
        private Boolean isBalanced;
    }
}
