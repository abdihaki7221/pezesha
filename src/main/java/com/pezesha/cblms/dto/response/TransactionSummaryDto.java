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
public class TransactionSummaryDto {
    private String transactionId;
    private Instant transactionDate;
    private String description;
    private String status;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private String currency;
    private int entriesCount;
}