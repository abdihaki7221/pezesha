package com.pezesha.cblms.service;

import com.pezesha.cblms.dto.request.CreateTransactionRequest;
import com.pezesha.cblms.dto.request.ReverseTransactionRequest;
import com.pezesha.cblms.dto.response.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * @author AOmar
 */
public interface TransactionManagementService {

    Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> getTransactionById(String transactionId);

    Mono<ResponseEntity<ResponseDto<ReversalResponse>>> reverseTransaction(String transactionId, ReverseTransactionRequest request);

    Mono<ResponseEntity<ResponseDto<TransactionListResponse>>> listTransactions(String startDate, String endDate, String accountId, String status, int page, int size);

    Mono<ResponseEntity<ResponseDto<AccountTransactionHistoryResponse>>> getAccountTransactionHistory(String accountId, String startDate, String endDate, int page, int size);
    Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> createTransaction(CreateTransactionRequest request);

}