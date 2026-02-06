package com.pezesha.cblms.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @author AOmar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {

    @NotBlank(message = "Idempotency key is required")
    @Size(min = 10, max = 100, message = "Idempotency key must be between 10 and 100 characters")
    private String idempotencyKey;

    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 500, message = "Description must be between 5 and 500 characters")
    private String description;

    @NotNull(message = "Transaction date is required")
    private Instant transactionDate;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(KES|UGX|USD)$", message = "Currency must be KES, UGX, or USD")
    private String currency;

    @NotEmpty(message = "At least 2 entries are required")
    @Size(min = 2, message = "At least 2 entries are required for double-entry")
    private List<JournalEntryRequest> entries;

    private Map<String, Object> metadata;
}
