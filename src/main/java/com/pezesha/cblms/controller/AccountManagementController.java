package com.pezesha.cblms.controller;

import com.pezesha.cblms.dto.request.AccountsRequestDto;
import com.pezesha.cblms.dto.request.UpdateAccountRequestDto;
import com.pezesha.cblms.dto.response.AccountCreationResponseDto;
import com.pezesha.cblms.dto.response.FetchAccountDetailsResponse;
import com.pezesha.cblms.dto.response.FetchAllAccountsResponse;
import com.pezesha.cblms.dto.response.ResponseDto;
import com.pezesha.cblms.service.AccountsManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author AOmar
 */
@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Account management APIs", description = "This APIs is All about Account Management such as creation,fetching accounts,balances")
@RequiredArgsConstructor
public class AccountManagementController {

    private final AccountsManagementService accountManagementService;

    @Operation(summary = "Create Accounts", description = "This Api creates an account")
    @PostMapping("/accounts/create")
    Mono<ResponseEntity<ResponseDto<AccountCreationResponseDto>>> createAccount(@Valid @RequestBody AccountsRequestDto accountsRequestDto) {
        return accountManagementService.createAccount(accountsRequestDto);
    }

    @Operation(summary = "fetch account details by id", description = "This Api fetch details of an account by using ID")
    @GetMapping("/accounts/{accountId}")
    Mono<ResponseEntity<ResponseDto<FetchAccountDetailsResponse>>> fetchAccountDetailsById(@Valid @Pattern(regexp = "\\d{10}", message = "Account ID is invalid") @PathVariable(value = "accountId") String accountId) {
        return accountManagementService.fetchAccountDetailsById(accountId);
    }


    @Operation(summary = "fetch all accounts using filters", description = "This Api fetches all accounts and their data using filters")
    @GetMapping("/accounts/fetchAll")
    Mono<ResponseEntity<ResponseDto<FetchAllAccountsResponse>>> fetchAllAccounts(

            @RequestParam(required = false)
            @Pattern(
                    regexp = "^(ASSET|LIABILITY|EQUITY|INCOME|EXPENSE)$",
                    message = "invalid fieldKey"
            )
            String type,

            @RequestParam(required = false)
            @Pattern(
                    regexp = "^(KES|USD|UGX)$",
                    message = "invalid fieldKey"
            )
            String currency,

            @RequestParam(required = false)
            boolean isActive,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return accountManagementService.fetchAllAccounts(type,
                currency,
                isActive,
                page,
                size);
    }

    @Operation(summary = "Update Account Details", description = "This API is used to update details of an account")
    @PutMapping("/account/updateAccount/{accountId}")
    Mono<ResponseEntity<ResponseDto<String>>> updateAccount(@Valid @Pattern(regexp = "\\d{10}", message = "Account ID is invalid")@PathVariable(value = "accountId") String accountId, @Valid @RequestBody UpdateAccountRequestDto updateAccountRequestDto) {
        return accountManagementService.updateAccount(accountId,updateAccountRequestDto);
    }


    @Operation(summary = "soft delete account")
    @DeleteMapping("/account/deleteAccount/{accountId}")
    Mono<ResponseEntity<ResponseDto<String>>> deleteAccount(@Valid @Pattern(regexp = "\\d{10}", message = "Account ID is invalid")@PathVariable(value = "accountId")String accountId) {


        return accountManagementService.deleteAccount(accountId);
    }


}
