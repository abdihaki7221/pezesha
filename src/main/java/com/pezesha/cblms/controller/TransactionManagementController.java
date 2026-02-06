package com.pezesha.cblms.controller;

import com.pezesha.cblms.dto.request.CreateTransactionRequest;
import com.pezesha.cblms.dto.request.ReverseTransactionRequest;
import com.pezesha.cblms.dto.response.*;
import com.pezesha.cblms.service.TransactionManagementService;
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
@Tag(name = "Transaction management APIs", description = "APIs for transaction management such as fetching by ID, reversing, listing, and account history")
@RequiredArgsConstructor
public class TransactionManagementController {

    private final TransactionManagementService transactionManagementService;
    @Operation(summary = "Create a new transaction", description = "Creates a new double-entry transaction with idempotency support")
    @PostMapping("/transactions")
    Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        return transactionManagementService.createTransaction(request);
    }

    @Operation(summary = "Get transaction by ID", description = "Fetch details of a transaction by its transaction ID")
    @GetMapping("/transactions/{transactionId}")
    Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> getTransactionById(
            @PathVariable String transactionId) {
        return transactionManagementService.getTransactionById(transactionId);
    }

    @Operation(summary = "Reverse a transaction", description = "Reverse an existing transaction using the provided details")
    @PostMapping("/transactions/{transactionId}/reverse")
    Mono<ResponseEntity<ResponseDto<ReversalResponse>>> reverseTransaction(
            @PathVariable String transactionId,
            @Valid @RequestBody ReverseTransactionRequest request) {
        return transactionManagementService.reverseTransaction(transactionId, request);
    }

    @Operation(summary = "List transactions with filters", description = "Fetch transactions using filters")
    @GetMapping("/transactions")
    Mono<ResponseEntity<ResponseDto<TransactionListResponse>>> listTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionManagementService.listTransactions(startDate, endDate, accountId, status, page, size);
    }

    @Operation(summary = "Get account transaction history", description = "Fetch transaction history for a specific account.")
    @GetMapping("/accounts/{accountId}/transactions")
    Mono<ResponseEntity<ResponseDto<AccountTransactionHistoryResponse>>> getAccountTransactionHistory(
            @Valid @Pattern(regexp = "\\d+", message = "Account ID is invalid") @PathVariable String accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionManagementService.getAccountTransactionHistory(accountId, startDate, endDate, page, size);
    }
}