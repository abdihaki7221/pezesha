package com.pezesha.cblms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author AOmar
 */
@Data
@Builder
public class AccountTransactionHistoryResponse {
    private String accountId;
    private String accountName;
    private String currency;
    private BigDecimal currentBalance;
    private List<AccountEntryDto> transactions;
    private Pagination pagination;
}