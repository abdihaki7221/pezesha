package com.pezesha.cblms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author AOmar
 */
@Data
@Builder
public class EntryDto {
    private String accountId;
    private String accountName;
    private String accountCode;
    private String accountType;
    private BigDecimal debit;
    private BigDecimal credit;
}
