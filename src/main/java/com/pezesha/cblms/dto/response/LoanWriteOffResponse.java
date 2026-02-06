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
public class LoanWriteOffResponse {

    private String writeOffId;

    private String loanId;

    private Instant writeOffDate;

    private String writeOffType;

    private AmountWrittenOff amountWrittenOff;

    private String reason;

    private String loanStatus;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmountWrittenOff {
        private BigDecimal principal;
        private BigDecimal interest;
        private BigDecimal total;
    }
}