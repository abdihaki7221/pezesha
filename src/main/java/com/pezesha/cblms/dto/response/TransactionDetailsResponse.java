package com.pezesha.cblms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @author AOmar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailsResponse {

    private String transactionId;
    private String idempotencyKey;
    private String status;
    private Instant transactionDate;
    private String description;
    private String currency;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private List<EntryDto> entries;
    private Map<String, String> metadata;
    private Instant postedAt;
    private Object reversalInfo; // null as per example
}