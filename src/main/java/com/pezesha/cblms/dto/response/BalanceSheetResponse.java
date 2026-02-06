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
public class BalanceSheetResponse {

    @JsonProperty("reportDate")
    private Instant reportDate;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("assets")
    private AssetSection assets;

    @JsonProperty("liabilities")
    private LiabilitySection liabilities;

    @JsonProperty("equity")
    private EquitySection equity;

    @JsonProperty("totals")
    private BalanceSheetTotals totals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetSection {
        @JsonProperty("currentAssets")
        private AccountGroup currentAssets;

        @JsonProperty("totalAssets")
        private BigDecimal totalAssets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiabilitySection {
        @JsonProperty("currentLiabilities")
        private AccountGroup currentLiabilities;

        @JsonProperty("totalLiabilities")
        private BigDecimal totalLiabilities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquitySection {
        @JsonProperty("accounts")
        private List<BalanceSheetAccount> accounts;

        @JsonProperty("totalEquity")
        private BigDecimal totalEquity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountGroup {
        @JsonProperty("accounts")
        private List<BalanceSheetAccount> accounts;

        @JsonProperty("total")
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceSheetAccount {
        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;

        @JsonProperty("balance")
        private BigDecimal balance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceSheetTotals {
        @JsonProperty("totalAssets")
        private BigDecimal totalAssets;

        @JsonProperty("totalLiabilitiesAndEquity")
        private BigDecimal totalLiabilitiesAndEquity;

        @JsonProperty("difference")
        private BigDecimal difference;

        @JsonProperty("isBalanced")
        private Boolean isBalanced;
    }
}
