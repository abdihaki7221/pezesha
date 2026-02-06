package com.pezesha.cblms.service.impl;

import com.pezesha.cblms.dto.request.AccountsRequestDto;
import com.pezesha.cblms.dto.request.UpdateAccountRequestDto;
import com.pezesha.cblms.dto.response.*;
import com.pezesha.cblms.models.Account;
import com.pezesha.cblms.repository.AccountsManagementRepository;
import com.pezesha.cblms.service.AccountsManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


/**
 * @author AOmar
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountsManagementServiceImpl implements AccountsManagementService {

    private final AccountsManagementRepository accountsManagementRepository;

    @Override
    public Mono<ResponseEntity<ResponseDto<AccountCreationResponseDto>>> createAccount(AccountsRequestDto accountsRequestDto) {

        if (accountsRequestDto.getParentAccountId() != null) {
            return accountsManagementRepository.findById(accountsRequestDto.getParentAccountId())
                    .flatMap(res -> {
                        //check for the account type
                        String parentType = res.getType();
                        String childType = accountsRequestDto.getAccountType();

                        if (!parentType.equals(childType)) {
                            log.info("Child account type should same as parent account type");
                            ResponseDto<AccountCreationResponseDto> responseDto = new ResponseDto<>();
                            responseDto.setStatus("BAD_REQUEST");
                            responseDto.setMessage("Child account type does not match with parent account type");
                            responseDto.setStatusCode("400");
                            return Mono.just(ResponseEntity.status(400).body(responseDto));
                        }
                        String parentCurrency = res.getCurrency();
                        String childCurrency = accountsRequestDto.getCurrency();
                        if (!parentCurrency.equals(childCurrency)) {
                            ResponseDto<AccountCreationResponseDto> responseDto = new ResponseDto<>();
                            responseDto.setStatus("BAD_REQUEST");
                            responseDto.setMessage("Child currency does not match with parent currency");
                            responseDto.setStatusCode("400");
                            return Mono.just(ResponseEntity.status(400).body(responseDto));
                        }

                        return validateAndCreateAccounts
                                (accountsRequestDto);

                    }).switchIfEmpty(processParentNotFound())
                    .onErrorResume(error -> getInternalServerErrorResponse(error, "an error occurred when getting parent account data : {}"));
        }

        return
                validateAndCreateAccounts
                        (accountsRequestDto);

    }

    @Override
    public Mono<ResponseEntity<ResponseDto<FetchAccountDetailsResponse>>> fetchAccountDetailsById(String accountId) {

        return accountsManagementRepository.findById(Long.valueOf(accountId))
                .flatMap(res -> {
                    FetchAccountDetailsResponse fetchAccountDetailsResponse = new FetchAccountDetailsResponse();
                    ParentAccount parentAccount = new ParentAccount();

                    if (res.getParentAccountId() != null) {
                        return accountsManagementRepository.findById(res.getParentAccountId())
                                .flatMap(result -> {
                                    parentAccount.setAccountId(String.valueOf(res.getParentAccountId()));
                                    parentAccount.setCode(result.getCode());
                                    parentAccount.setName(result.getName());

                                    fetchAccountDetailsResponse.setParentAccount(parentAccount);

                                    fetchAccountDetailsResponse.setAccountId(accountId);
                                    fetchAccountDetailsResponse.setCode(res.getCode());
                                    fetchAccountDetailsResponse.setType(res.getType());
                                    fetchAccountDetailsResponse.setName(res.getName());
                                    fetchAccountDetailsResponse.setCurrency(res.getCurrency());
                                    fetchAccountDetailsResponse.setCreatedAt(res.getCreatedAt());
                                    fetchAccountDetailsResponse.setCurrentBalance(res.getCurrentBalance());
                                    fetchAccountDetailsResponse.setHasTransactions(res.isHasTransaction());
                                    fetchAccountDetailsResponse.setActive(res.isActive());
                                    fetchAccountDetailsResponse.setDeleted(res.isDeleted());
                                    ResponseDto<FetchAccountDetailsResponse> responseDto = new ResponseDto<>();
                                    responseDto.setStatus("OK");
                                    responseDto.setBody(fetchAccountDetailsResponse);
                                    responseDto.setStatusCode("200");
                                    responseDto.setMessage("successfully fetched account details");
                                    return Mono.just(ResponseEntity.ok(responseDto));

                                });
                    }


                    fetchAccountDetailsResponse.setAccountId(accountId);
                    fetchAccountDetailsResponse.setCode(res.getCode());
                    fetchAccountDetailsResponse.setName(res.getName());
                    fetchAccountDetailsResponse.setCurrency(res.getCurrency());
                    fetchAccountDetailsResponse.setCreatedAt(res.getCreatedAt());
                    fetchAccountDetailsResponse.setCurrentBalance(res.getCurrentBalance());
                    fetchAccountDetailsResponse.setHasTransactions(res.isActive());
                    fetchAccountDetailsResponse.setActive(res.isActive());


                    ResponseDto<FetchAccountDetailsResponse> responseDto = new ResponseDto<>();
                    responseDto.setStatus("OK");
                    responseDto.setBody(fetchAccountDetailsResponse);
                    responseDto.setStatusCode("200");
                    responseDto.setMessage("successfully fetched account details");
                    return Mono.just(ResponseEntity.ok(responseDto));

                })
                .switchIfEmpty(processParentAccountCodeNotFound())
                .onErrorResume(error -> {
                    ResponseDto<FetchAccountDetailsResponse> responseDto = new ResponseDto<>();
                    responseDto.setStatus("INTERNAL_SERVER_ERROR");
                    responseDto.setMessage(error.getLocalizedMessage());
                    responseDto.setStatusCode("500");
                    log.error("an error occurred {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(responseDto));
                });
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<FetchAllAccountsResponse>>> fetchAllAccounts(
            String type,
            String currency,
            Boolean isActive,
            int page,
            int size) {

        int validatedSize = Math.min(size, 100);
        int skip = page * validatedSize;

        return accountsManagementRepository.findAll()


                .filter(acc -> type == null || acc.getType().equalsIgnoreCase(type))
                .filter(acc -> currency == null || acc.getCurrency().equalsIgnoreCase(currency))
                .filter(acc -> isActive == null || acc.isActive() == isActive)

                .collectList()
                .flatMap(allFiltered -> {

                    long totalElements = allFiltered.size();
                    int totalPages = (int) Math.ceil((double) totalElements / validatedSize);


                    List<FetchAccountDetailsResponse> paged =
                            allFiltered.stream()
                                    .skip(skip)
                                    .limit(validatedSize)
                                    .map(acc -> FetchAccountDetailsResponse.builder()
                                            .accountId(String.valueOf(acc.getId()))
                                            .code(acc.getCode())
                                            .name(acc.getName())
                                            .type(acc.getType())
                                            .currency(acc.getCurrency())
                                            .currentBalance(acc.getCurrentBalance())
                                            .isActive(acc.isActive())
                                            .isDeleted(acc.isDeleted())
                                            .hasTransactions(acc.isHasTransaction())
                                            .createdAt(acc.getCreatedAt())
                                            .build())
                                    .toList();

                    FetchAllAccountsResponse responseBody =
                            FetchAllAccountsResponse.builder()
                                    .accounts(paged)
                                    .currentPage(page)
                                    .pageSize(validatedSize)
                                    .totalPages(totalPages)
                                    .totalElements(totalElements)
                                    .build();

                    ResponseDto<FetchAllAccountsResponse> responseDto = new ResponseDto<>();
                    responseDto.setStatus("OK");
                    responseDto.setStatusCode("200");
                    responseDto.setMessage("Accounts fetched successfully");
                    responseDto.setBody(responseBody);

                    return Mono.just(ResponseEntity.ok(responseDto));
                })

                .onErrorResume(error -> {
                    log.error("Error fetching accounts {}", error.getMessage(), error);

                    ResponseDto<FetchAllAccountsResponse> responseDto = new ResponseDto<>();
                    responseDto.setStatus("INTERNAL_SERVER_ERROR");
                    responseDto.setStatusCode("500");
                    responseDto.setMessage(error.getLocalizedMessage());

                    return Mono.just(ResponseEntity.internalServerError().body(responseDto));
                });
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<String>>> updateAccount(String accountId, UpdateAccountRequestDto updateAccountRequestDto) {
        return accountsManagementRepository.findById(Long.valueOf(accountId))
                .flatMap(res->{
                    ResponseDto<String> responseDto = new ResponseDto<>();

                    log.info("successfully the account with id {}", accountId);
                    if (updateAccountRequestDto.getAccountName()!=null){
                        res.setName(updateAccountRequestDto.getAccountName());
                    }
                    if (res.isActive()==updateAccountRequestDto.isActive()){
                        responseDto.setStatus("BAD_REQUEST");
                        responseDto.setMessage("account is already active");
                        responseDto.setStatusCode("400");
                        return Mono.just(ResponseEntity.badRequest().body(responseDto));
                    }
                    res.setActive(updateAccountRequestDto.isActive());
                    res.setUpdatedAt(LocalDateTime.now());
                    accountsManagementRepository.save(res).subscribe();

                    responseDto.setStatus("OK");
                    responseDto.setMessage("account updated successfully");
                    responseDto.setStatusCode("200");
                    responseDto.setBody("account updated successfully");
                    return Mono.just(ResponseEntity.ok(responseDto));
                }).switchIfEmpty(processUpdateAccountNotFound())
                .onErrorResume(error->{
                    log.error("an error occurred {}", error.getMessage());
                    ResponseDto<String> responseDto = new ResponseDto<>();
                    responseDto.setStatus("INTERNAL_SERVER_ERROR");
                    responseDto.setMessage(error.getLocalizedMessage());
                    responseDto.setStatusCode("500");
                    responseDto.setMessage(error.getLocalizedMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(responseDto));
                });
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<String>>> deleteAccount(String accountId) {

        return accountsManagementRepository.findByParentAccountId(Long.valueOf(accountId))
                .flatMap(result->{
                    ResponseDto<String> responseDto = new ResponseDto<>();
                    responseDto.setStatus("BAD_REQUEST");
                    responseDto.setMessage("account has active children");
                    responseDto.setStatusCode("400");
                    return Mono.just(ResponseEntity.badRequest().body(responseDto));
                })
                .switchIfEmpty(processAndValidateAccount(accountId))
                .onErrorResume(error->{
                    log.error("an error occurred {}", error.getMessage());
                    ResponseDto<String> responseDto = new ResponseDto<>();
                    responseDto.setStatus("INTERNAL_SERVER_ERROR");
                    responseDto.setStatusCode("500");
                    responseDto.setMessage(error.getLocalizedMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(responseDto));
                });

    }

    private Mono<ResponseEntity<ResponseDto<String>>> processAndValidateAccount(String accountId) {
        return accountsManagementRepository.findById(Long.valueOf(accountId))
                .flatMap(res->{
                    if (res.isHasTransaction()){
                        log.info("account id {} has active transaction", accountId);
                        ResponseDto<String> responseDto = new ResponseDto<>();
                        responseDto.setStatus("BAD_REQUEST");
                        responseDto.setMessage("account has active transactions");
                        responseDto.setStatusCode("400");
                        return Mono.just(ResponseEntity.badRequest().body(responseDto));
                    }

                    log.info("current balance {}, big decimal of zero {}",res.getCurrentBalance(),BigDecimal.valueOf(0));

                    if (res.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0) {
                        log.info("account {} balance is greater than zero", accountId);
                        ResponseDto<String> responseDto = new ResponseDto<>();
                        responseDto.setStatus("BAD_REQUEST");
                        responseDto.setMessage("account balance is greater than zero");
                        responseDto.setStatusCode("400");
                        return Mono.just(ResponseEntity.badRequest().body(responseDto));
                    }

                    if (res.isDeleted()){
                        log.info("account {} already deleted", accountId);
                        ResponseDto<String> responseDto = new ResponseDto<>();
                        responseDto.setStatus("BAD_REQUEST");
                        responseDto.setMessage("account is already deleted");
                        responseDto.setStatusCode("400");
                        return Mono.just(ResponseEntity.badRequest().body(responseDto));
                    }

                    res.setActive(false);
                    res.setUpdatedAt(LocalDateTime.now());
                    res.setHasTransaction(false);
                    res.setDeleted(true);
                    accountsManagementRepository.save(res).subscribe();

                    ResponseDto<String> responseDto = new ResponseDto<>();
                    responseDto.setStatus("OK");
                    responseDto.setMessage("account deleted successfully");
                    responseDto.setStatusCode("200");
                    responseDto.setBody("account deleted successfully");
                    return Mono.just(ResponseEntity.ok(responseDto));

                }).onErrorResume(error->{
                    log.error("an error occurred {}", error.getMessage());
                    ResponseDto<String> responseDto = new ResponseDto<>();
                    responseDto.setStatus("INTERNAL_SERVER_ERROR");
                    responseDto.setMessage(error.getLocalizedMessage());
                    responseDto.setStatusCode("500");
                    return Mono.just(ResponseEntity.internalServerError().body(responseDto));

                });
    }

    private Mono<ResponseEntity<ResponseDto<String>>> processUpdateAccountNotFound() {
        log.info("account not found for account id");
        ResponseDto<String> responseDto = new ResponseDto<>();
        responseDto.setStatus("NOT_FOUND");
        responseDto.setMessage("account not found for account id");
        responseDto.setStatusCode("404");
        return Mono.just(ResponseEntity.ok(responseDto));
    }


    private Mono<ResponseEntity<ResponseDto<FetchAccountDetailsResponse>>> processParentAccountCodeNotFound() {
        log.info("parent account not found for the parent id provided");
        ResponseDto<FetchAccountDetailsResponse> responseDto = new ResponseDto<>();
        responseDto.setStatus("NOT_FOUND");
        responseDto.setMessage("Parent account not found");
        responseDto.setStatusCode("404");
        return Mono.just(ResponseEntity.status(404).body(responseDto));
    }


    private Mono<ResponseEntity<ResponseDto<AccountCreationResponseDto>>> validateAndCreateAccounts(AccountsRequestDto accountsRequestDto) {
        return accountsManagementRepository.findByCode(accountsRequestDto.getAccountCode())
                .flatMap(this::processAccountAlreadyExists)
                .switchIfEmpty(processAccountCreation(accountsRequestDto))
                .onErrorResume(error -> getInternalServerErrorResponse(error, "an error occurred when get account data : {}caused by {}"));
    }

    private static Mono<ResponseEntity<ResponseDto<AccountCreationResponseDto>>> getInternalServerErrorResponse(Throwable error, String s) {
        ResponseDto<AccountCreationResponseDto> responseDto = new ResponseDto<>();
        responseDto.setStatus("INTERNAL_SERVER_ERROR");
        responseDto.setMessage(error.getLocalizedMessage());
        responseDto.setStatusCode("500");
        log.error(s, error.getMessage(), error.getCause());
        return Mono.just(ResponseEntity.internalServerError().body(responseDto));
    }

    private Mono<ResponseEntity<ResponseDto<AccountCreationResponseDto>>> processAccountCreation(AccountsRequestDto accountsRequestDto) {
        log.info("Processing account creation");

        Account account = Account.builder()
                .parentAccountId(accountsRequestDto.getParentAccountId())
                .code(accountsRequestDto.getAccountCode())
                .type(accountsRequestDto.getAccountType())
                .name(accountsRequestDto.getAccountName())
                .currency(accountsRequestDto.getCurrency())
                .currentBalance(BigDecimal.ZERO)
                .isActive(false)
                .hasTransaction(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .createdBy(accountsRequestDto.getCreatorUsername())
                .build();

        return accountsManagementRepository.save(account)
                .doOnSubscribe(subscription -> log.info("Saving account to database: {}", account.getCode()))
                .doOnError(error -> log.error("Failed to save account {}: {}", account.getCode(), error.getMessage(), error))
                .doOnSuccess(savedAccount -> {
                    assert savedAccount != null;
                    log.info("Account saved successfully with ID: {}", savedAccount.getId());
                })
                .map(savedAccount -> {
                    AccountCreationResponseDto responseBody = new AccountCreationResponseDto();
                    responseBody.setAccountId(String.valueOf(savedAccount.getId()));
                    responseBody.setAccountName(savedAccount.getName());
                    responseBody.setAccountType(savedAccount.getType());
                    responseBody.setCurrency(savedAccount.getCurrency());
                    responseBody.setCurrentBalance(savedAccount.getCurrentBalance());
                    responseBody.setParentAccountId(String.valueOf(savedAccount.getParentAccountId()));
                    responseBody.setCreatedAt(savedAccount.getCreatedAt());

                    ResponseDto<AccountCreationResponseDto> responseDto = new ResponseDto<>();
                    responseDto.setStatus("CREATED");
                    responseDto.setMessage("Account created successfully");
                    responseDto.setStatusCode("201");
                    responseDto.setBody(responseBody);

                    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
                });
    }


    private Mono<ResponseEntity<ResponseDto<AccountCreationResponseDto>>> processAccountAlreadyExists(Account result) {
        log.info("Account already Exists for the account code: {}", result.getCode());
        ResponseDto<AccountCreationResponseDto> responseDto = new ResponseDto<>();
        responseDto.setStatus("BAD_REQUEST");
        responseDto.setMessage("Account already Exists for the account code");
        responseDto.setStatusCode("400");
        return Mono.just(ResponseEntity.status(400).body(responseDto));
    }

    private Mono<ResponseEntity<ResponseDto<AccountCreationResponseDto>>> processParentNotFound() {
        log.info("parent account not found for parent id provided");
        ResponseDto<AccountCreationResponseDto> responseDto = new ResponseDto<>();
        responseDto.setStatus("NOT_FOUND");
        responseDto.setMessage("Parent account not found");
        responseDto.setStatusCode("404");
        return Mono.just(ResponseEntity.status(404).body(responseDto));
    }
}
