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
public class RepaymentHistoryDto {
    private String repaymentId;
    private BigDecimal amount;
    private Instant date;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
}
