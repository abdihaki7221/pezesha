package com.pezesha.cblms.dto.request;

import com.pezesha.cblms.enums.WriteOffType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author AOmar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanWriteOffRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "Write-off date is required")
    private Instant writeOffDate;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;

    @NotNull(message = "Write-off type is required")
    private WriteOffType writeOffType;

    @DecimalMin(value = "0.0", message = "Partial amount must be non-negative")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal partialAmount;
}