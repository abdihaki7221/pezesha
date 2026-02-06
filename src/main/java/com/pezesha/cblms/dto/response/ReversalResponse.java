package com.pezesha.cblms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * @author AOmar
 */
@Data
@Builder
public class ReversalResponse {
    private String reversalTransactionId;
    private String originalTransactionId;
    private String status;
    private Instant reversalDate;
    private String reason;
    private List<EntryDto> entries;
    private Instant postedAt;
}