package com.pezesha.cblms.dto.request;

import com.pezesha.cblms.enums.AllocationStrategy;
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
public class LoanRepaymentRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be positive")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    private Instant paymentDate;

    @NotBlank(message = "Payment account ID is required")
    private String paymentAccountId;

    @NotNull(message = "Allocation strategy is required")
    private AllocationStrategy allocationStrategy;
}