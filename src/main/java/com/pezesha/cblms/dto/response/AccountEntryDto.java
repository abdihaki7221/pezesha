package com.pezesha.cblms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author AOmar
 */
@Data
@Builder
public class AccountEntryDto {
    private String transactionId;
    private Instant date;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;
}