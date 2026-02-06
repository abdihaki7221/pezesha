package com.pezesha.cblms.controller;

import com.pezesha.cblms.dto.response.*;
import com.pezesha.cblms.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/v1/reports")
@Validated
@Tag(name = "Reporting APIs", description = "Financial and operational reports including trial balance, balance sheet, loan aging, and income statement")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @Operation(
            summary = "Generate Trial Balance Report",
            description = "Generates a trial balance report showing all accounts with their debit and credit balances as of a specific date"
    )
    @GetMapping("/trial-balance")
    public Mono<ResponseEntity<ResponseDto<TrialBalanceResponse>>> getTrialBalance(
            @RequestParam
            @NotNull(message = "asOfDate is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant asOfDate,

            @RequestParam(required = false)
            String currency) {
        return reportingService.generateTrialBalance(asOfDate, currency);
    }

    @Operation(
            summary = "Generate Balance Sheet Report",
            description = "Generates a balance sheet showing assets, liabilities, and equity as of a specific date"
    )
    @GetMapping("/balance-sheet")
    public Mono<ResponseEntity<ResponseDto<BalanceSheetResponse>>> getBalanceSheet(
            @RequestParam
            @NotNull(message = "asOfDate is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant asOfDate,

            @RequestParam(required = false)
            String currency) {
        return reportingService.generateBalanceSheet(asOfDate, currency);
    }

    @Operation(
            summary = "Generate Loan Aging Report",
            description = "Generates a loan aging report showing loans categorized by days overdue (Current, 1-30, 31-60, 61-90, 90+)"
    )
    @GetMapping("/loan-aging")
    public Mono<ResponseEntity<ResponseDto<LoanAgingResponse>>> getLoanAging(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant asOfDate,

            @RequestParam(required = false)
            String currency,

            @RequestParam(required = false)
            String status) {
        return reportingService.generateLoanAging(asOfDate, currency, status);
    }

    @Operation(
            summary = "Generate Income Statement Report",
            description = "Generates an income statement showing revenues, expenses, and net income for a specific period"
    )
    @GetMapping("/income-statement")
    public Mono<ResponseEntity<ResponseDto<IncomeStatementResponse>>> getIncomeStatement(
            @RequestParam
            @NotNull(message = "startDate is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant startDate,

            @RequestParam
            @NotNull(message = "endDate is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant endDate,

            @RequestParam(required = false)
            String currency) {
        return reportingService.generateIncomeStatement(startDate, endDate, currency);
    }
}
