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
public class IncomeStatementResponse {

    @JsonProperty("periodStart")
    private Instant periodStart;

    @JsonProperty("periodEnd")
    private Instant periodEnd;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("income")
    private IncomeSection income;

    @JsonProperty("expenses")
    private ExpenseSection expenses;

    @JsonProperty("netIncome")
    private BigDecimal netIncome;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncomeSection {
        @JsonProperty("interestIncome")
        private AccountGroup interestIncome;

        @JsonProperty("feeIncome")
        private AccountGroup feeIncome;

        @JsonProperty("totalIncome")
        private BigDecimal totalIncome;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseSection {
        @JsonProperty("operatingExpenses")
        private AccountGroup operatingExpenses;

        @JsonProperty("totalExpenses")
        private BigDecimal totalExpenses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountGroup {
        @JsonProperty("accounts")
        private List<IncomeStatementAccount> accounts;

        @JsonProperty("total")
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncomeStatementAccount {
        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;

        @JsonProperty("amount")
        private BigDecimal amount;
    }
}
