package com.pezesha.cblms.dto.request;

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
public class LoanDisbursementRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotBlank(message = "Borrower ID is required")
    private String borrowerId;

    @NotBlank(message = "borrower Name is required")
    private String borrowerName;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "100.0", message = "Principal must be at least 100")
    @DecimalMax(value = "10000000.0", message = "Principal cannot exceed 10,000,000")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal interestRate;

    @NotNull(message = "Origination fee is required")
    @DecimalMin(value = "0.0", message = "Origination fee must be non-negative")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal originationFee;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(KES|UGX|USD)$", message = "Currency must be KES, UGX, or USD")
    private String currency;

    @NotNull(message = "Disbursement date is required")
    private Instant disbursementDate;

    @NotNull(message = "Maturity date is required")
    private Instant maturityDate;



    @NotBlank(message = "Disbursement account ID is required")
    private String disbursementAccountId;
}