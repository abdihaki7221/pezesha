package com.pezesha.cblms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author AOmar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryRequest {
    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotNull(message = "Debit amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Debit must be non-negative")
    @Digits(integer = 15, fraction = 2, message = "Debit must have max 2 decimal places")
    private BigDecimal debit;

    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Credit must be non-negative")
    @Digits(integer = 15, fraction = 2, message = "Credit must have max 2 decimal places")
    private BigDecimal credit;
}
