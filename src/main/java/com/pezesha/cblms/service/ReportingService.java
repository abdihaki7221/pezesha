package com.pezesha.cblms.service;

import com.pezesha.cblms.dto.response.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.time.Instant;
/**
 * @author AOmar
 */
public interface ReportingService {


    Mono<ResponseEntity<ResponseDto<TrialBalanceResponse>>> generateTrialBalance(
            Instant asOfDate,
            String currency);

    Mono<ResponseEntity<ResponseDto<BalanceSheetResponse>>> generateBalanceSheet(
            Instant asOfDate,
            String currency);


    Mono<ResponseEntity<ResponseDto<LoanAgingResponse>>> generateLoanAging(
            Instant asOfDate,
            String currency,
            String status);

    Mono<ResponseEntity<ResponseDto<IncomeStatementResponse>>> generateIncomeStatement(
            Instant startDate,
            Instant endDate,
            String currency);
}
