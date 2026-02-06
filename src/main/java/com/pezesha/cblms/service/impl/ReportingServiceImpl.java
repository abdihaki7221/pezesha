package com.pezesha.cblms.service.impl;

import com.pezesha.cblms.dto.response.*;

import com.pezesha.cblms.exceptions.ExcessiveDateRangeException;
import com.pezesha.cblms.exceptions.InvalidDateException;
import com.pezesha.cblms.exceptions.UnbalancedBalanceSheetException;
import com.pezesha.cblms.exceptions.UnbalancedTrialBalanceException;
import com.pezesha.cblms.models.JournalEntry;
import com.pezesha.cblms.repository.JournalEntryRepository;
import com.pezesha.cblms.repository.LoanRepository;
import com.pezesha.cblms.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
/**
 * @author AOmar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private final JournalEntryRepository journalEntryRepository;
    private final LoanRepository loanRepository;

    @Override
    public Mono<ResponseEntity<ResponseDto<TrialBalanceResponse>>> generateTrialBalance(
            Instant asOfDate,
            String currency) {

        log.info("Generating trial balance report for date: {}, currency: {}", asOfDate, currency);

        if (asOfDate.isAfter(Instant.now())) {
            return Mono.error(new InvalidDateException("Cannot generate report for future date"));
        }

        return journalEntryRepository.findAllEntriesUpToDate(asOfDate, currency)
                .collectList()
                .<ResponseEntity<ResponseDto<TrialBalanceResponse>>>handle((entries, sink) -> {
                    Map<String, TrialBalanceResponse.TrialBalanceAccount> accountMap = new HashMap<>();

                    entries.forEach(entry -> {
                        String accountKey = entry.getAccountCode();
                        accountMap.putIfAbsent(accountKey, TrialBalanceResponse.TrialBalanceAccount.builder()
                                .accountId(String.valueOf(entry.getAccountId()))
                                .code(entry.getAccountCode())
                                .name(entry.getAccountName())
                                .type(entry.getAccountType())
                                .debit(BigDecimal.ZERO)
                                .credit(BigDecimal.ZERO)
                                .build());

                        TrialBalanceResponse.TrialBalanceAccount account = accountMap.get(accountKey);
                        account.setDebit(account.getDebit().add(entry.getDebit()));
                        account.setCredit(account.getCredit().add(entry.getCredit()));
                    });

                    List<TrialBalanceResponse.TrialBalanceAccount> accounts = new ArrayList<>(accountMap.values());

                    BigDecimal totalDebit = accounts.stream()
                            .map(TrialBalanceResponse.TrialBalanceAccount::getDebit)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalCredit = accounts.stream()
                            .map(TrialBalanceResponse.TrialBalanceAccount::getCredit)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal difference = totalDebit.subtract(totalCredit);
                    boolean isBalanced = difference.compareTo(BigDecimal.ZERO) == 0;

                    if (!isBalanced) {
                        log.error("Trial balance is unbalanced! Difference: {}", difference);
                        sink.error(new UnbalancedTrialBalanceException(
                                "Trial balance does not match. Difference: " + difference));
                        return;
                    }

                    TrialBalanceResponse.TrialBalanceTotals totals = TrialBalanceResponse.TrialBalanceTotals.builder()
                            .totalDebit(totalDebit)
                            .totalCredit(totalCredit)
                            .difference(difference)
                            .isBalanced(isBalanced)
                            .build();

                    TrialBalanceResponse response = TrialBalanceResponse.builder()
                            .reportDate(asOfDate)
                            .currency(currency != null ? currency : "KES")
                            .accounts(accounts)
                            .totals(totals)
                            .build();

                    sink.next(ResponseEntity.ok(ResponseDto.<TrialBalanceResponse>builder()
                            .status("OK")
                            .message("Trial balance report generated successfully")
                            .statusCode("200")
                            .body(response)
                            .build()));
                })
                .doOnSuccess(result -> log.info("Trial balance report generated successfully"))
                .doOnError(error -> log.error("Error generating trial balance: {}", error.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<BalanceSheetResponse>>> generateBalanceSheet(
            Instant asOfDate,
            String currency) {

        log.info("Generating balance sheet report for date: {}, currency: {}", asOfDate, currency);

        if (asOfDate.isAfter(Instant.now())) {
            return Mono.error(new InvalidDateException("Cannot generate report for future date"));
        }

        return journalEntryRepository.findAllEntriesUpToDate(asOfDate, currency)
                .collectList()
                .<ResponseEntity<ResponseDto<BalanceSheetResponse>>>handle((entries, sink) -> {
                    Map<String, List<BalanceSheetResponse.BalanceSheetAccount>> accountsByType = new HashMap<>();

                    entries.stream()
                            .collect(Collectors.groupingBy(entry -> entry.getAccountCode()))
                            .forEach((code, accountEntries) -> {
                                var firstEntry = accountEntries.get(0);
                                String type = firstEntry.getAccountType();

                                BigDecimal debitTotal = accountEntries.stream()
                                        .map(e -> e.getDebit())
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                BigDecimal creditTotal = accountEntries.stream()
                                        .map(e -> e.getCredit())
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                BigDecimal balance;
                                if ("ASSET".equals(type) || "EXPENSE".equals(type)) {
                                    balance = debitTotal.subtract(creditTotal);
                                } else {
                                    balance = creditTotal.subtract(debitTotal);
                                }

                                BalanceSheetResponse.BalanceSheetAccount account =
                                        BalanceSheetResponse.BalanceSheetAccount.builder()
                                                .code(code)
                                                .name(firstEntry.getAccountName())
                                                .balance(balance)
                                                .build();

                                accountsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(account);
                            });

                    List<BalanceSheetResponse.BalanceSheetAccount> assetAccounts =
                            accountsByType.getOrDefault("ASSET", new ArrayList<>());
                    BigDecimal totalAssets = assetAccounts.stream()
                            .map(BalanceSheetResponse.BalanceSheetAccount::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BalanceSheetResponse.AssetSection assets = BalanceSheetResponse.AssetSection.builder()
                            .currentAssets(BalanceSheetResponse.AccountGroup.builder()
                                    .accounts(assetAccounts)
                                    .total(totalAssets)
                                    .build())
                            .totalAssets(totalAssets)
                            .build();

                    List<BalanceSheetResponse.BalanceSheetAccount> liabilityAccounts =
                            accountsByType.getOrDefault("LIABILITY", new ArrayList<>());
                    BigDecimal totalLiabilities = liabilityAccounts.stream()
                            .map(BalanceSheetResponse.BalanceSheetAccount::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BalanceSheetResponse.LiabilitySection liabilities =
                            BalanceSheetResponse.LiabilitySection.builder()
                                    .currentLiabilities(BalanceSheetResponse.AccountGroup.builder()
                                            .accounts(liabilityAccounts)
                                            .total(totalLiabilities)
                                            .build())
                                    .totalLiabilities(totalLiabilities)
                                    .build();

                    List<BalanceSheetResponse.BalanceSheetAccount> equityAccounts =
                            accountsByType.getOrDefault("EQUITY", new ArrayList<>());
                    BigDecimal totalEquity = equityAccounts.stream()
                            .map(BalanceSheetResponse.BalanceSheetAccount::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BalanceSheetResponse.EquitySection equity = BalanceSheetResponse.EquitySection.builder()
                            .accounts(equityAccounts)
                            .totalEquity(totalEquity)
                            .build();

                    BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
                    BigDecimal difference = totalAssets.subtract(totalLiabilitiesAndEquity);
                    boolean isBalanced = difference.compareTo(BigDecimal.ZERO) == 0;

                    if (!isBalanced) {
                        log.error("Balance sheet is unbalanced! Assets: {}, L+E: {}, Difference: {}",
                                totalAssets, totalLiabilitiesAndEquity, difference);
                        sink.error(new UnbalancedBalanceSheetException(
                                "Balance sheet equation does not balance. Assets: " + totalAssets +
                                        ", Liabilities + Equity: " + totalLiabilitiesAndEquity));
                        return;
                    }

                    BalanceSheetResponse.BalanceSheetTotals totals =
                            BalanceSheetResponse.BalanceSheetTotals.builder()
                                    .totalAssets(totalAssets)
                                    .totalLiabilitiesAndEquity(totalLiabilitiesAndEquity)
                                    .difference(difference)
                                    .isBalanced(isBalanced)
                                    .build();

                    BalanceSheetResponse response = BalanceSheetResponse.builder()
                            .reportDate(asOfDate)
                            .currency(currency != null ? currency : "KES")
                            .assets(assets)
                            .liabilities(liabilities)
                            .equity(equity)
                            .totals(totals)
                            .build();

                    sink.next(ResponseEntity.ok(ResponseDto.<BalanceSheetResponse>builder()
                            .status("OK")
                            .statusCode("200")
                            .message("Balance sheet report generated successfully")
                            .body(response)
                            .build()));
                })
                .doOnSuccess(result -> log.info("Balance sheet report generated successfully"))
                .doOnError(error -> log.error("Error generating balance sheet: {}", error.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<LoanAgingResponse>>> generateLoanAging(
            Instant asOfDate,
            String currency,
            String status) {

        Instant reportDate = asOfDate != null ? asOfDate : Instant.now();
        log.info("Generating loan aging report for date: {}, currency: {}, status: {}",
                reportDate, currency, status);

        return loanRepository.findActiveLoans(currency, status)
                .collectList()
                .map(loans -> {
                    Map<String, LoanAgingResponse.AgingBucket> buckets = initializeAgingBuckets();
                    List<LoanAgingResponse.AgingLoan> agingLoans = new ArrayList<>();

                    BigDecimal totalOverdue = BigDecimal.ZERO;

                    for (var loan : loans) {
                        long daysOverdue = 0;
                        if (loan.getMaturityDate() != null && reportDate.isAfter(loan.getMaturityDate())) {
                            daysOverdue = ChronoUnit.DAYS.between(loan.getMaturityDate(), reportDate);
                        }

                        String agingCategory = determineAgingCategory(daysOverdue);

                        BigDecimal totalOutstanding = loan.getOutstandingPrincipal()
                                .add(loan.getOutstandingInterest());

                        LoanAgingResponse.AgingLoan agingLoan = LoanAgingResponse.AgingLoan.builder()
                                .loanId(loan.getLoanId())
                                .borrowerId(loan.getBorrowerId())
                                .principalAmount(loan.getPrincipalAmount())
                                .outstandingPrincipal(loan.getOutstandingPrincipal())
                                .outstandingInterest(loan.getOutstandingInterest())
                                .totalOutstanding(totalOutstanding)
                                .disbursementDate(loan.getDisbursementDate())
                                .maturityDate(loan.getMaturityDate())
                                .daysOverdue(daysOverdue)
                                .agingCategory(agingCategory)
                                .status(loan.getStatus())
                                .build();

                        agingLoans.add(agingLoan);

                        LoanAgingResponse.AgingBucket bucket = buckets.get(agingCategory);
                        bucket.setCount(bucket.getCount() + 1);
                        bucket.setTotalOutstanding(bucket.getTotalOutstanding().add(totalOutstanding));

                        if (daysOverdue > 0) {
                            totalOverdue = totalOverdue.add(totalOutstanding);
                        }
                    }

                    LoanAgingResponse.AgingSummary summary = LoanAgingResponse.AgingSummary.builder()
                            .current(buckets.get("CURRENT"))
                            .days1to30(buckets.get("DAYS_1_TO_30"))
                            .days31to60(buckets.get("DAYS_31_TO_60"))
                            .days61to90(buckets.get("DAYS_61_TO_90"))
                            .over90Days(buckets.get("OVER_90_DAYS"))
                            .build();

                    BigDecimal totalOutstanding = buckets.values().stream()
                            .map(LoanAgingResponse.AgingBucket::getTotalOutstanding)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    LoanAgingResponse.AgingTotals totals = LoanAgingResponse.AgingTotals.builder()
                            .totalLoans(loans.size())
                            .totalOutstanding(totalOutstanding)
                            .totalOverdue(totalOverdue)
                            .build();

                    LoanAgingResponse response = LoanAgingResponse.builder()
                            .reportDate(reportDate)
                            .currency(currency != null ? currency : "KES")
                            .summary(summary)
                            .loans(agingLoans)
                            .totals(totals)
                            .build();

                    return ResponseEntity.ok(ResponseDto.<LoanAgingResponse>builder()
                            .status("OK")
                            .statusCode("200")
                            .message("Loan aging report generated successfully")
                            .body(response)
                            .build());
                })
                .doOnSuccess(result -> log.info("Loan aging report generated successfully"))
                .doOnError(error -> log.error("Error generating loan aging report: {}", error.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<IncomeStatementResponse>>> generateIncomeStatement(
            Instant startDate,
            Instant endDate,
            String currency) {

        log.info("Generating income statement for period {} to {}, currency: {}",
                startDate, endDate, currency);

        if (endDate.isBefore(startDate)) {
            return Mono.error(new InvalidDateException("End date must be after start date"));
        }

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 365) {
            return Mono.error(new ExcessiveDateRangeException("Cannot query more than 1 year"));
        }

        return journalEntryRepository.findEntriesBetweenDates(startDate, endDate, currency)
                .collectList()
                .map(entries -> {
                    Map<String, List<IncomeStatementResponse.IncomeStatementAccount>> incomeAccounts =
                            new HashMap<>();
                    Map<String, List<IncomeStatementResponse.IncomeStatementAccount>> expenseAccounts =
                            new HashMap<>();

                    entries.stream()
                            .collect(Collectors.groupingBy(JournalEntry::getAccountCode))
                            .forEach((code, accountEntries) -> {
                                var firstEntry = accountEntries.get(0);
                                String type = firstEntry.getAccountType();

                                BigDecimal amount = accountEntries.stream()
                                        .map(e -> {
                                            if ("INCOME".equals(type)) {
                                                return e.getCredit().subtract(e.getDebit());
                                            } else if ("EXPENSE".equals(type)) {
                                                return e.getDebit().subtract(e.getCredit());
                                            }
                                            return BigDecimal.ZERO;
                                        })
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                IncomeStatementResponse.IncomeStatementAccount account =
                                        IncomeStatementResponse.IncomeStatementAccount.builder()
                                                .code(code)
                                                .name(firstEntry.getAccountName())
                                                .amount(amount)
                                                .build();

                                if ("INCOME".equals(type)) {
                                    String category = determineIncomeCategory(code);
                                    incomeAccounts.computeIfAbsent(category, k -> new ArrayList<>())
                                            .add(account);
                                } else if ("EXPENSE".equals(type)) {
                                    expenseAccounts.computeIfAbsent("OPERATING", k -> new ArrayList<>())
                                            .add(account);
                                }
                            });

                    List<IncomeStatementResponse.IncomeStatementAccount> interestIncomeAccounts =
                            incomeAccounts.getOrDefault("INTEREST", new ArrayList<>());
                    BigDecimal interestIncomeTotal = interestIncomeAccounts.stream()
                            .map(IncomeStatementResponse.IncomeStatementAccount::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<IncomeStatementResponse.IncomeStatementAccount> feeIncomeAccounts =
                            incomeAccounts.getOrDefault("FEE", new ArrayList<>());
                    BigDecimal feeIncomeTotal = feeIncomeAccounts.stream()
                            .map(IncomeStatementResponse.IncomeStatementAccount::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalIncome = interestIncomeTotal.add(feeIncomeTotal);

                    IncomeStatementResponse.IncomeSection income =
                            IncomeStatementResponse.IncomeSection.builder()
                                    .interestIncome(IncomeStatementResponse.AccountGroup.builder()
                                            .accounts(interestIncomeAccounts)
                                            .total(interestIncomeTotal)
                                            .build())
                                    .feeIncome(IncomeStatementResponse.AccountGroup.builder()
                                            .accounts(feeIncomeAccounts)
                                            .total(feeIncomeTotal)
                                            .build())
                                    .totalIncome(totalIncome)
                                    .build();

                    List<IncomeStatementResponse.IncomeStatementAccount> operatingExpenseAccounts =
                            expenseAccounts.getOrDefault("OPERATING", new ArrayList<>());
                    BigDecimal totalExpenses = operatingExpenseAccounts.stream()
                            .map(IncomeStatementResponse.IncomeStatementAccount::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    IncomeStatementResponse.ExpenseSection expenses =
                            IncomeStatementResponse.ExpenseSection.builder()
                                    .operatingExpenses(IncomeStatementResponse.AccountGroup.builder()
                                            .accounts(operatingExpenseAccounts)
                                            .total(totalExpenses)
                                            .build())
                                    .totalExpenses(totalExpenses)
                                    .build();

                    BigDecimal netIncome = totalIncome.subtract(totalExpenses);

                    IncomeStatementResponse response = IncomeStatementResponse.builder()
                            .periodStart(startDate)
                            .periodEnd(endDate)
                            .currency(currency != null ? currency : "KES")
                            .income(income)
                            .expenses(expenses)
                            .netIncome(netIncome)
                            .build();

                    return ResponseEntity.ok(ResponseDto.<IncomeStatementResponse>builder()
                            .status("OK")
                            .message("Income statement report generated successfully")
                            .statusCode("200")
                            .body(response)
                            .build());
                })
                .doOnSuccess(result -> log.info("Income statement report generated successfully"))
                .doOnError(error -> log.error("Error generating income statement: {}", error.getMessage()));
    }


    private Map<String, LoanAgingResponse.AgingBucket> initializeAgingBuckets() {
        Map<String, LoanAgingResponse.AgingBucket> buckets = new HashMap<>();

        buckets.put("CURRENT", LoanAgingResponse.AgingBucket.builder()
                .count(0)
                .totalOutstanding(BigDecimal.ZERO)
                .build());

        buckets.put("DAYS_1_TO_30", LoanAgingResponse.AgingBucket.builder()
                .count(0)
                .totalOutstanding(BigDecimal.ZERO)
                .build());

        buckets.put("DAYS_31_TO_60", LoanAgingResponse.AgingBucket.builder()
                .count(0)
                .totalOutstanding(BigDecimal.ZERO)
                .build());

        buckets.put("DAYS_61_TO_90", LoanAgingResponse.AgingBucket.builder()
                .count(0)
                .totalOutstanding(BigDecimal.ZERO)
                .build());

        buckets.put("OVER_90_DAYS", LoanAgingResponse.AgingBucket.builder()
                .count(0)
                .totalOutstanding(BigDecimal.ZERO)
                .build());

        return buckets;
    }

    private String determineAgingCategory(long daysOverdue) {
        if (daysOverdue == 0) {
            return "CURRENT";
        } else if (daysOverdue <= 30) {
            return "DAYS_1_TO_30";
        } else if (daysOverdue <= 60) {
            return "DAYS_31_TO_60";
        } else if (daysOverdue <= 90) {
            return "DAYS_61_TO_90";
        } else {
            return "OVER_90_DAYS";
        }
    }

    private String determineIncomeCategory(String accountCode) {

        if (accountCode.startsWith("41")) {
            return "INTEREST";
        } else if (accountCode.startsWith("42")) {
            return "FEE";
        }
        return "INTEREST";
    }
}
