package com.pezesha.cblms.controller;

import com.pezesha.cblms.dto.request.LoanDisbursementRequest;
import com.pezesha.cblms.dto.request.LoanRepaymentRequest;
import com.pezesha.cblms.dto.request.LoanWriteOffRequest;
import com.pezesha.cblms.dto.response.LoanDetailsResponse;
import com.pezesha.cblms.dto.response.LoanDisbursementResponse;
import com.pezesha.cblms.dto.response.LoanListResponse;
import com.pezesha.cblms.dto.response.LoanRepaymentResponse;
import com.pezesha.cblms.dto.response.LoanWriteOffResponse;
import com.pezesha.cblms.dto.response.ResponseDto;
import com.pezesha.cblms.service.LoanManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * @author AOmar
 */
@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Loan Management APIs", description = "APIs for loan management such as disbursement, repayment, write-off, and fetching loans")
@RequiredArgsConstructor
public class LoanManagementController {

    private final LoanManagementService loanManagementService;

    @Operation(
            summary = "Disburse a loan",
            description = "Creates and disburses a new loan with journal entries for principal disbursement and origination fee"
    )
    @PostMapping("/loans/disburse")
    public Mono<ResponseEntity<ResponseDto<LoanDisbursementResponse>>> disburseLoan(
            @Valid @RequestBody LoanDisbursementRequest request) {
        return loanManagementService.disburseLoan(request);
    }


    @Operation(
            summary = "Process loan repayment",
            description = "Processes a repayment for a loan with allocation strategy (INTEREST_FIRST, PRINCIPAL_FIRST, PROPORTIONAL) and posts journal entries"
    )
    @PostMapping("/loans/{loanId}/repayments")
    public Mono<ResponseEntity<ResponseDto<LoanRepaymentResponse>>> processRepayment(
            @PathVariable String loanId,
            @Valid @RequestBody LoanRepaymentRequest request) {
        return loanManagementService.processRepayment(loanId, request);
    }


    @Operation(
            summary = "Write off loan",
            description = "Writes off a loan (full or partial) and posts journal entries for bad debt expense"
    )
    @PostMapping("/loans/{loanId}/write-off")
    public Mono<ResponseEntity<ResponseDto<LoanWriteOffResponse>>> writeOffLoan(
            @PathVariable String loanId,
            @Valid @RequestBody LoanWriteOffRequest request) {
        return loanManagementService.writeOffLoan(loanId, request);
    }


    @Operation(
            summary = "Get loan details",
            description = "Retrieves detailed information about a loan including repayment history"
    )
    @GetMapping("/loans/{loanId}")
    public Mono<ResponseEntity<ResponseDto<LoanDetailsResponse>>> getLoanDetails(
            @PathVariable String loanId) {
        return loanManagementService.getLoanDetails(loanId);
    }


    @Operation(
            summary = "List loans",
            description = "Retrieves a paginated list of loans with optional filters for borrower, status, and disbursement date range"
    )
    @GetMapping("/loans")
    public Mono<ResponseEntity<ResponseDto<LoanListResponse>>> listLoans(
            @RequestParam(required = false) String borrowerId,

            @RequestParam(required = false) String status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant endDate,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be non-negative")
            Integer page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size cannot exceed 100")
            Integer size) {
        return loanManagementService.listLoans(borrowerId, status, startDate, endDate, page, size);
    }
}