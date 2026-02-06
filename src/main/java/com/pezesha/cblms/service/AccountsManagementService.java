package com.pezesha.cblms.service;

import com.pezesha.cblms.dto.request.AccountsRequestDto;
import com.pezesha.cblms.dto.request.UpdateAccountRequestDto;
import com.pezesha.cblms.dto.response.AccountCreationResponseDto;
import com.pezesha.cblms.dto.response.FetchAccountDetailsResponse;
import com.pezesha.cblms.dto.response.FetchAllAccountsResponse;
import com.pezesha.cblms.dto.response.ResponseDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * @author AOmar
 */
public interface AccountsManagementService {
    Mono<ResponseEntity<ResponseDto<AccountCreationResponseDto>>> createAccount(AccountsRequestDto accountsRequestDto);

    Mono<ResponseEntity<ResponseDto<FetchAccountDetailsResponse>>> fetchAccountDetailsById(String accountId);

    Mono<ResponseEntity<ResponseDto<FetchAllAccountsResponse>>> fetchAllAccounts(String type, String currency, Boolean isActive, int page, int size);

    Mono<ResponseEntity<ResponseDto<String>>> updateAccount(String accountId,  UpdateAccountRequestDto updateAccountRequestDto);

    Mono<ResponseEntity<ResponseDto<String>>> deleteAccount(String accountId);
}
