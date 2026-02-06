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
public class AccountCreationResponseDto {

    private String accountId;
    private String accountName;
    private String accountType;
    private String currency;
    private String parentAccountId;
    private BigDecimal currentBalance;
    private LocalDateTime createdAt;

}
