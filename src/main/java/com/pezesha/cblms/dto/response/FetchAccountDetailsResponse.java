package com.pezesha.cblms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author AOmar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FetchAccountDetailsResponse {
    private String accountId;
    private String code;
    private String name;
    private String type;
    private String currency;
    private BigDecimal currentBalance;
    private boolean isActive;
    private boolean hasTransactions;
    private boolean isDeleted;
    private ParentAccount parentAccount;
    private LocalDateTime createdAt;
}
