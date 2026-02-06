package com.pezesha.cblms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author AOmar
 */
@Data
@Builder
public class TransactionListResponse {
    private List<TransactionSummaryDto> transactions;
    private Pagination pagination;
}