package com.pezesha.cblms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UpdateAccountRequestDto {

    @Size(min = 3, max = 100, message = "Invalid Account name")
    private String accountName;
    @NotNull(message = "isActive field is required")
    private boolean isActive;
}
