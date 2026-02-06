package com.pezesha.cblms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author AOmar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountsRequestDto {

    @NotBlank(message = "Account code is required")
    @Pattern(regexp = "^[0-9]{4,10}$", message = "Invalid Account code")
    private String accountCode;
    @NotBlank(message = "Account name is required")
    @Size(min = 3, max = 100, message = "Invalid Account name")
    private String accountName;
    @NotNull(message = "Account type is required")
    @Pattern(regexp = "^(ASSET|LIABILITY|EQUITY|INCOME|EXPENSE)$", message = "account type is invalid")
    private String accountType;
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(KES|UGX|USD)$", message = "Currency must be KES, UGX, or USD")
    private String currency;

    private Long parentAccountId;
    private String creatorUsername;
}
