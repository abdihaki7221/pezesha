package com.pezesha.cblms.service.impl;

import com.pezesha.cblms.dto.request.CreateTransactionRequest;
import com.pezesha.cblms.dto.request.JournalEntryRequest;
import com.pezesha.cblms.dto.request.ReverseTransactionRequest;
import com.pezesha.cblms.dto.response.*;
import com.pezesha.cblms.exceptions.ServiceValidation;
import com.pezesha.cblms.models.*;
import com.pezesha.cblms.repository.*;
import com.pezesha.cblms.service.TransactionManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author AOmar
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionManagementServiceImpl implements TransactionManagementService {

    private final TransactionRepository transactionRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final AccountsManagementRepository accountsManagementRepository;

    @Override
    public Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> createTransaction(CreateTransactionRequest request) {
        return idempotencyRepository.findByKey(request.getIdempotencyKey())
                .flatMap(imp -> transactionRepository.findById(imp.getTransactionId())
                        .flatMap(tx -> journalEntryRepository.findByTransactionId(tx.getId()).collectList()
                                .map(entries -> buildTransactionDetailsResponse(tx, entries)))
                        .map(details -> {

                            log.info("transaction ID: {} already processed", details.getTransactionId());
                            ResponseDto<TransactionDetailsResponse> dto = new ResponseDto<>();
                            dto.setStatus("OK");
                            dto.setStatusCode("200");
                            dto.setMessage("Transaction already processed");
                            dto.setBody(details);
                            return ResponseEntity.ok(dto);
                        }))
                .switchIfEmpty(doCreateNewTransaction(request))
                .onErrorResume(OptimisticLockingFailureException.class, e -> processConcurrentUpdateConflict())
                .onErrorResume(error -> getInternalServerErrorResponse(error, "Error creating transaction: {}"));
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> getTransactionById(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .flatMap(tx -> journalEntryRepository.findByTransactionId(tx.getId())
                        .collectList()
                        .map(entries -> buildTransactionDetailsResponse(tx, entries)))
                .map(details -> {
                    log.info("transaction id: {} details fetched successfully", transactionId);
                    ResponseDto<TransactionDetailsResponse> responseDto = new ResponseDto<>();
                    responseDto.setStatus("OK");
                    responseDto.setStatusCode("200");
                    responseDto.setMessage("Transaction fetched successfully");
                    responseDto.setBody(details);
                    return ResponseEntity.ok(responseDto);
                })
                .switchIfEmpty(processTransactionNotFound())
                .onErrorResume(error -> getInternalServerErrorResponse(error, "Error fetching transaction: {}"));
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<ReversalResponse>>> reverseTransaction(String transactionId, ReverseTransactionRequest request) {
        return transactionRepository.findByTransactionId(transactionId)
                .flatMap(tx -> {
                    if (tx.getReversalTransactionId() != null) {
                        return processTransactionAlreadyReversed(tx);
                    }
                    Instant now = Instant.now();
                    if (request.getReversalDate().isBefore(tx.getTransactionDate())) {
                        return processInvalidReversalDate("Reversal date cannot be before original transaction date");
                    }
                    if (request.getReversalDate().isAfter(now)) {
                        return processInvalidReversalDate("Reversal date cannot be in the future");
                    }
                    return handleIdempotency(request.getIdempotencyKey(), tx, request);
                })
                .switchIfEmpty(processTransactionNotFound())
                .onErrorResume(error -> getInternalServerErrorResponse(error, "Error reversing transaction: {}"));
    }

    private Mono<ResponseEntity<ResponseDto<ReversalResponse>>> handleIdempotency(String idempotencyKey, Transaction originalTx, ReverseTransactionRequest request) {
        return idempotencyRepository.findByKey(idempotencyKey)
                .flatMap(existing -> transactionRepository.findById(existing.getTransactionId())
                        .flatMap(this::buildReversalResponse)
                        .map(response -> {
                            ResponseDto<ReversalResponse> dto = new ResponseDto<>();
                            dto.setStatus("OK");
                            dto.setStatusCode("200");
                            dto.setMessage("Reversal already processed");
                            dto.setBody(response);
                            return ResponseEntity.ok(dto);
                        }))
                .switchIfEmpty(createAndSaveReversal(originalTx, request));
    }

    private Mono<ResponseEntity<ResponseDto<ReversalResponse>>> createAndSaveReversal(Transaction originalTx, ReverseTransactionRequest request) {
        Instant now = Instant.now();
        log.info("transaction is being reversed");
        Transaction reversalTx = Transaction.builder()
                .transactionId("txn_" + UUID.randomUUID().toString().substring(0, 8))
                .idempotencyKey(request.getIdempotencyKey())
                .status("POSTED")
                .transactionDate(request.getReversalDate())
                .description("Reversal of " + originalTx.getDescription())
                .currency(originalTx.getCurrency())
                .totalDebit(originalTx.getTotalCredit())
                .totalCredit(originalTx.getTotalDebit())
                .postedAt(now)
                .createdAt(LocalDateTime.now())
                .reversedBy("system")
                .reason(request.getReason())
                .build();

        return journalEntryRepository.findByTransactionId(originalTx.getId()).collectList()
                .flatMap(originalEntries -> checkSufficientBalanceForReversal(originalEntries)
                        .then(transactionRepository.save(reversalTx)))
                .flatMap(savedReversal -> saveIdempotency(request.getIdempotencyKey(), savedReversal.getId())
                        .then(journalEntryRepository.findByTransactionId(originalTx.getId()).collectList())
                        .flatMap(originalEntries -> {
                            List<JournalEntry> reversedEntries = originalEntries.stream()
                                    .map(entry -> JournalEntry.builder()
                                            .transactionId(savedReversal.getId())
                                            .accountId(entry.getAccountId())
                                            .accountCode(entry.getAccountCode())
                                            .accountName(entry.getAccountName())
                                            .accountType(entry.getAccountType())
                                            .debit(entry.getCredit())
                                            .credit(entry.getDebit())
                                            .transactionDate(savedReversal.getTransactionDate())
                                            .build())
                                    .collect(Collectors.toList());

                            return journalEntryRepository.saveAll(reversedEntries).then(Mono.just(originalEntries));
                        })
                        .flatMap(originalEntries -> {
                            originalTx.setStatus("REVERSED");
                            originalTx.setReversedBy("system");
                            originalTx.setReversalTransactionId(savedReversal.getId());
                            originalTx.setReversalDate(request.getReversalDate());
                            originalTx.setUpdatedAt(LocalDateTime.now());
                            return transactionRepository.save(originalTx).then(Mono.just(savedReversal));
                        })
                        .flatMap(this::buildReversalResponse))
                .map(response -> {
                    ResponseDto<ReversalResponse> dto = new ResponseDto<>();
                    dto.setStatus("CREATED");
                    dto.setStatusCode("201");
                    dto.setMessage("Transaction reversed successfully");
                    dto.setBody(response);
                    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
                });
    }

    private Mono<Void> checkSufficientBalanceForReversal(List<JournalEntry> originalEntries) {
        return Flux.fromIterable(originalEntries)
                .flatMap(entry -> accountsManagementRepository.findById(entry.getAccountId())
                        .flatMap(acc -> {
                            BigDecimal reversalCredit = entry.getDebit();
                            if (reversalCredit.compareTo(BigDecimal.ZERO) > 0 && acc.getCurrentBalance().compareTo(reversalCredit) < 0) {
                                return Mono.error(new ServiceValidation("Insufficient balance in account " + acc.getId() + " for reversal credit of " + reversalCredit));
                            }
                            return Mono.just(acc);
                        }))
                .then();
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<TransactionListResponse>>> listTransactions(String startDate, String endDate, String accountId, String status, int page, int size) {
        int validatedSize = Math.min(size, 100);
        int skip = page * validatedSize;

        Mono<List<Transaction>> txMono;
        if (accountId != null) {
            long accId;
            try {
                accId = Long.parseLong(accountId);
            } catch (NumberFormatException e) {
                return processInvalidAccountId();
            }
            txMono = journalEntryRepository.findByAccountId(accId)
                    .map(JournalEntry::getTransactionId)
                    .distinct()
                    .flatMap(transactionRepository::findById)
                    .collectList();
        } else {
            txMono = transactionRepository.findAll().collectList();
        }

        return txMono
                .map(allTx -> allTx.stream()
                        .filter(tx -> startDate == null || !tx.getTransactionDate().isBefore(Instant.parse(startDate)))
                        .filter(tx -> endDate == null || !tx.getTransactionDate().isAfter(Instant.parse(endDate)))
                        .filter(tx -> status == null || tx.getStatus().equals(status))
                        .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                        .collect(Collectors.toList()))
                .flatMap(filtered -> Mono.just(filtered)
                        .zipWith(Flux.fromIterable(filtered)
                                .skip(skip)
                                .take(validatedSize)
                                .flatMap(tx -> journalEntryRepository.countByTransactionId(tx.getId())
                                        .map(count -> buildTransactionSummary(tx, count.intValue())))
                                .collectList(), (all, paged) -> {
                            long total = all.size();
                            int totalPages = (int) Math.ceil((double) total / validatedSize);

                            Pagination pagination = new Pagination();
                            pagination.setCurrentPage(page);
                            pagination.setTotalPages(totalPages);
                            pagination.setTotalElements(total);
                            pagination.setPageSize(validatedSize);


                            TransactionListResponse body = TransactionListResponse.builder()
                                    .transactions(paged)
                                    .pagination(pagination)
                                    .build();

                            log.info("transactions fetched successfully");
                            ResponseDto<TransactionListResponse> dto = new ResponseDto<>();
                            dto.setStatus("OK");
                            dto.setStatusCode("200");
                            dto.setMessage("Transactions fetched successfully");
                            dto.setBody(body);
                            return ResponseEntity.ok(dto);
                        }))
                .onErrorResume(error -> getInternalServerErrorResponse(error, "Error listing transactions: {}"));
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<AccountTransactionHistoryResponse>>> getAccountTransactionHistory(String accountIdStr, String startDate, String endDate, int page, int size) {
        long accountId;
        try {
            accountId = Long.parseLong(accountIdStr);
        } catch (NumberFormatException e) {
            return processInvalidAccountId();
        }

        log.info("fetching transaction history");
        return accountsManagementRepository.findById(accountId)
                .flatMap(acc -> {
                    Flux<JournalEntry> entriesFlux = journalEntryRepository.findByAccountId(accountId);
                    if (startDate != null || endDate != null) {
                        entriesFlux = entriesFlux.filter(e -> {
                            Instant d = e.getTransactionDate();
                            boolean ok = true;
                            if (startDate != null) ok &= !d.isBefore(Instant.parse(startDate));
                            if (endDate != null) ok &= !d.isAfter(Instant.parse(endDate));
                            return ok;
                        });
                    }
                    return entriesFlux.collectList()
                            .map(list -> list.stream()
                                    .sorted(Comparator.comparing(JournalEntry::getTransactionDate).reversed())
                                    .collect(Collectors.toList()))
                            .flatMap(sorted -> {
                                Set<Long> txIds = sorted.stream().map(JournalEntry::getTransactionId).collect(Collectors.toSet());
                                return transactionRepository.findAllById(txIds).collectMap(Transaction::getId)
                                        .map(txMap -> {
                                            List<AccountEntryDto> dtos = new ArrayList<>();
                                            BigDecimal running = acc.getCurrentBalance();
                                            for (JournalEntry e : sorted) {
                                                Transaction tx = txMap.get(e.getTransactionId());
                                                if (tx == null) continue;
                                                AccountEntryDto dto = AccountEntryDto.builder()
                                                        .transactionId(tx.getTransactionId())
                                                        .date(e.getTransactionDate())
                                                        .description(tx.getDescription())
                                                        .debit(e.getDebit())
                                                        .credit(e.getCredit())
                                                        .runningBalance(running)
                                                        .build();
                                                dtos.add(dto);
                                                running = running.add(e.getCredit()).subtract(e.getDebit());
                                            }
                                            int validatedSize = Math.min(size, 100);
                                            int skip = page * validatedSize;
                                            List<AccountEntryDto> paged = dtos.stream().skip(skip).limit(validatedSize).collect(Collectors.toList());
                                            long total = dtos.size();
                                            int totalPages = (int) Math.ceil((double) total / validatedSize);
                                            AccountTransactionHistoryResponse body = AccountTransactionHistoryResponse.builder()
                                                    .accountId(accountIdStr)
                                                    .accountName(acc.getName())
                                                    .currency(acc.getCurrency())
                                                    .currentBalance(acc.getCurrentBalance())
                                                    .transactions(paged)
                                                    .pagination(Pagination.builder()
                                                            .currentPage(page)
                                                            .pageSize(validatedSize)
                                                            .totalPages(totalPages)
                                                            .totalElements(total)
                                                            .build())
                                                    .build();

                                            log.info("account transaction history fetched successfully");
                                            ResponseDto<AccountTransactionHistoryResponse> dto = new ResponseDto<>();
                                            dto.setStatus("OK");
                                            dto.setStatusCode("200");
                                            dto.setMessage("Account transaction history fetched successfully");
                                            dto.setBody(body);
                                            return ResponseEntity.ok(dto);
                                        });
                            });
                })
                .switchIfEmpty(processAccountNotFound())
                .onErrorResume(error -> getInternalServerErrorResponse(error, "Error fetching account history: {}"));
    }

    private Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> doCreateNewTransaction(CreateTransactionRequest request) {
        Instant now = Instant.now();
        if (request.getTransactionDate().isAfter(now)) {
            log.info("transaction ID: with idempotency key {} date cannot be in the future", request.getIdempotencyKey());

            return processInvalidTransactionDate("Transaction date cannot be in the future");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (JournalEntryRequest entry : request.getEntries()) {
            boolean hasDebit = entry.getDebit().compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = entry.getCredit().compareTo(BigDecimal.ZERO) > 0;
            if (hasDebit && hasCredit) {
                log.info("Journal Entry cannot have both debit and credit non-zero");
                return processInvalidJournalEntry("Entry cannot have both debit and credit non-zero");
            }
            if (!hasDebit && !hasCredit) {
                log.info("Journal Entry cannot have both debit and credit");
                return processInvalidJournalEntry("Entry must have either debit or credit non-zero");
            }
            totalDebit = totalDebit.add(entry.getDebit());
            totalCredit = totalCredit.add(entry.getCredit());
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            return processUnbalancedTransaction(totalDebit, totalCredit);
        }

        BigDecimal finalTotalCredit = totalCredit;
        BigDecimal finalTotalDebit = totalDebit;
        return Flux.fromIterable(request.getEntries())
                .flatMap(entry -> {
                    try {
                        Long accId = Long.valueOf(entry.getAccountId());
                        return accountsManagementRepository.findById(accId)
                                .switchIfEmpty(Mono.error(new ServiceValidation("Account not found: " + entry.getAccountId())));
                    } catch (NumberFormatException ex) {

                        return Mono.error(new ServiceValidation("Invalid account ID format: " + entry.getAccountId()));
                    }
                })
                .collectList()
                .flatMap(accounts -> {
                    for (Account acc : accounts) {
                        if (!acc.getCurrency().equals(request.getCurrency())) {
                            return Mono.error(new ServiceValidation("Currency mismatch for account " + acc.getId() + ": expected " + request.getCurrency() + " but was " + acc.getCurrency()));
                        }
                    }
                    return checkSufficientBalance(request.getEntries(), accounts)
                            .thenReturn(accounts);
                })
                .flatMap(accounts -> postNewTransaction(request, accounts, finalTotalDebit, finalTotalCredit))
                .flatMap(savedTx -> journalEntryRepository.findByTransactionId(savedTx.getId()).collectList()
                        .map(entries -> buildTransactionDetailsResponse(savedTx, entries)))
                .map(details -> {
                    log.info("Transaction details fetched successfully");
                    ResponseDto<TransactionDetailsResponse> dto = new ResponseDto<>();
                    dto.setStatus("CREATED");
                    dto.setStatusCode("201");
                    dto.setMessage("Transaction created successfully");
                    dto.setBody(details);
                    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
                });
    }

    private Mono<Void> checkSufficientBalance(List<JournalEntryRequest> entries, List<Account> accounts) {
        for (int i = 0; i < entries.size(); i++) {
            JournalEntryRequest entry = entries.get(i);
            Account acc = accounts.get(i);
            if (entry.getCredit().compareTo(BigDecimal.ZERO) > 0 && acc.getCurrentBalance().compareTo(entry.getCredit()) < 0) {
                return Mono.error(new ServiceValidation("Insufficient balance in account " + acc.getId() + " for credit of " + entry.getCredit()));
            }
        }
        return Mono.empty();
    }

    private Mono<Transaction> postNewTransaction(CreateTransactionRequest request, List<Account> accounts, BigDecimal totalDebit, BigDecimal totalCredit) {
        String txnId = "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Instant now = Instant.now();

        log.info("posting a new transaction....");
        Map<String, String> metadataMap = null;
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            metadataMap = request.getMetadata().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
        }
        Transaction tx = Transaction.builder()
                .transactionId(txnId)
                .idempotencyKey(request.getIdempotencyKey())
                .status("POSTED")
                .transactionDate(request.getTransactionDate())
                .description(request.getDescription())
                .currency(request.getCurrency())
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .postedAt(now)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(tx)
                .flatMap(savedTx -> {
                    List<JournalEntry> entries = new ArrayList<>();
                    for (int i = 0; i < request.getEntries().size(); i++) {
                        JournalEntryRequest reqEntry = request.getEntries().get(i);
                        Account acc = accounts.get(i);
                        JournalEntry je = JournalEntry.builder()
                                .transactionId(savedTx.getId())
                                .accountId(acc.getId())
                                .accountCode(acc.getCode())
                                .accountName(acc.getName())
                                .accountType(acc.getType())
                                .debit(reqEntry.getDebit())
                                .credit(reqEntry.getCredit())
                                .transactionDate(savedTx.getTransactionDate())
                                .build();
                        entries.add(je);
                    }
                    return journalEntryRepository.saveAll(entries).then(Mono.just(savedTx));
                })
                .flatMap(savedTx -> {
                    List<Account> updatedAccounts = new ArrayList<>();
                    for (int i = 0; i < request.getEntries().size(); i++) {
                        JournalEntryRequest reqEntry = request.getEntries().get(i);
                        Account acc = accounts.get(i).toBuilder().build();
                        BigDecimal delta = reqEntry.getDebit().subtract(reqEntry.getCredit());
                        acc.setCurrentBalance(acc.getCurrentBalance().add(delta));
                        acc.setHasTransaction(true);
                        acc.setActive(true);
                        acc.setUpdatedAt(LocalDateTime.now());
                        updatedAccounts.add(acc);
                    }
                    return accountsManagementRepository.saveAll(updatedAccounts).then(Mono.just(savedTx));
                })
                .flatMap(savedTx -> saveIdempotency(request.getIdempotencyKey(), savedTx.getId()).then(Mono.just(savedTx)));
    }




    private <T> Mono<ResponseEntity<ResponseDto<T>>> processInvalidTransactionDate(String msg) {
        log.info("Invalid transaction date: {}", msg);
        ResponseDto<T> dto = new ResponseDto<>();
        dto.setStatus("BAD_REQUEST");
        dto.setMessage(msg);
        dto.setStatusCode("400");
        return Mono.just(ResponseEntity.badRequest().body(dto));
    }

    private <T> Mono<ResponseEntity<ResponseDto<T>>> processInvalidJournalEntry(String msg) {
        log.info("Invalid journal entry: {}", msg);
        ResponseDto<T> dto = new ResponseDto<>();
        dto.setStatus("BAD_REQUEST");
        dto.setMessage(msg);
        dto.setStatusCode("400");
        return Mono.just(ResponseEntity.badRequest().body(dto));
    }

    private <T> Mono<ResponseEntity<ResponseDto<T>>> processUnbalancedTransaction(BigDecimal debit, BigDecimal credit) {
        String msg = String.format("Debits (%.2f) must equal Credits (%.2f)", debit, credit);
        log.info("Unbalanced transaction: {}", msg);
        ResponseDto<T> dto = new ResponseDto<>();
        dto.setStatus("BAD_REQUEST");
        dto.setMessage(msg);
        dto.setStatusCode("400");
        return Mono.just(ResponseEntity.badRequest().body(dto));
    }

    private <T> Mono<ResponseEntity<ResponseDto<T>>> processConcurrentUpdateConflict() {
        String msg = "Concurrent update conflict on account balance. Please retry.";
        log.info("Optimistic locking failure: {}", msg);
        ResponseDto<T> dto = new ResponseDto<>();
        dto.setStatus("CONFLICT");
        dto.setMessage(msg);
        dto.setStatusCode("409");
        return Mono.just(ResponseEntity.status(409).body(dto));
    }

    private TransactionDetailsResponse buildTransactionDetailsResponse(Transaction tx, List<JournalEntry> entries) {
        List<EntryDto> entryDtos = entries.stream()
                .map(e -> EntryDto.builder()
                        .accountId(String.valueOf(e.getAccountId()))
                        .accountName(e.getAccountName())
                        .accountCode(e.getAccountCode())
                        .accountType(e.getAccountType())
                        .debit(e.getDebit())
                        .credit(e.getCredit())
                        .build())
                .collect(Collectors.toList());
        return TransactionDetailsResponse.builder()
                .transactionId(tx.getTransactionId())
                .idempotencyKey(tx.getIdempotencyKey())
                .status(tx.getStatus())
                .transactionDate(tx.getTransactionDate())
                .description(tx.getDescription())
                .currency(tx.getCurrency())
                .totalDebit(tx.getTotalDebit())
                .totalCredit(tx.getTotalCredit())
                .entries(entryDtos)
                .postedAt(tx.getPostedAt())
                .reversalInfo(null)
                .build();
    }

    private Mono<ReversalResponse> buildReversalResponse(Transaction tx) {
        return journalEntryRepository.findByTransactionId(tx.getId()).collectList()
                .map(entries -> {
                    List<EntryDto> entryDtos = entries.stream()
                            .map(e -> EntryDto.builder()
                                    .accountId(String.valueOf(e.getAccountId()))
                                    .accountName(e.getAccountName())
                                    .accountCode(e.getAccountCode())
                                    .accountType(e.getAccountType())
                                    .debit(e.getDebit())
                                    .credit(e.getCredit())
                                    .build())
                            .collect(Collectors.toList());
                    return ReversalResponse.builder()
                            .reversalTransactionId(tx.getTransactionId())
                            .originalTransactionId("")
                            .status(tx.getStatus())
                            .reversalDate(tx.getTransactionDate())
                            .reason(tx.getReason())
                            .entries(entryDtos)
                            .postedAt(tx.getPostedAt())
                            .build();
                });
    }

    private TransactionSummaryDto buildTransactionSummary(Transaction tx, int entriesCount) {
        return TransactionSummaryDto.builder()
                .transactionId(tx.getTransactionId())
                .transactionDate(tx.getTransactionDate())
                .description(tx.getDescription())
                .status(tx.getStatus())
                .totalDebit(tx.getTotalDebit())
                .totalCredit(tx.getTotalCredit())
                .currency(tx.getCurrency())
                .entriesCount(entriesCount)
                .build();
    }

    private Mono<Void> saveIdempotency(String key, Long txId) {
        Idempotency imp = Idempotency.builder()
                .key(key)
                .transactionId(txId)
                .createdAt(Instant.now())
                .build();
        return idempotencyRepository.save(imp).then();
    }

    private <T> Mono<ResponseEntity<ResponseDto<T>>> processTransactionNotFound() {
        log.info("Transaction not found for transactionId");
        ResponseDto<T> responseDto = new ResponseDto<>();
        responseDto.setStatus("NOT_FOUND");
        responseDto.setMessage("Transaction not found");
        responseDto.setStatusCode("404");
        return Mono.just(ResponseEntity.status(404).body(responseDto));
    }

    private Mono<ResponseEntity<ResponseDto<ReversalResponse>>> processTransactionAlreadyReversed(Transaction tx) {
        log.info("Transaction already reversed: {}", tx.getTransactionId());
        ResponseDto<ReversalResponse> responseDto = new ResponseDto<>();
        responseDto.setStatus("CONFLICT");
        responseDto.setMessage("Transaction already reversed by tx id: " + tx.getReversalTransactionId());
        responseDto.setStatusCode("409");
        return Mono.just(ResponseEntity.status(409).body(responseDto));
    }

    private  Mono<ResponseEntity<ResponseDto<ReversalResponse>>> processInvalidReversalDate(String msg) {
        log.info("Invalid reversal date: {}", msg);
        ResponseDto<ReversalResponse> responseDto = new ResponseDto<>();
        responseDto.setStatus("BAD_REQUEST");
        responseDto.setMessage(msg);
        responseDto.setStatusCode("400");
        return Mono.just(ResponseEntity.badRequest().body(responseDto));
    }

    private <T> Mono<ResponseEntity<ResponseDto<T>>> processInvalidAccountId() {
        log.info("invalid account ID format");
        ResponseDto<T> responseDto = new ResponseDto<>();
        responseDto.setStatus("BAD_REQUEST");
        responseDto.setMessage("Invalid account ID format");
        responseDto.setStatusCode("400");
        return Mono.just(ResponseEntity.badRequest().body(responseDto));
    }

    private <T> Mono<ResponseEntity<ResponseDto<T>>> processAccountNotFound() {
        log.info("Account not found");
        ResponseDto<T> responseDto = new ResponseDto<>();
        responseDto.setStatus("NOT_FOUND");
        responseDto.setMessage("Account not found");
        responseDto.setStatusCode("404");
        return Mono.just(ResponseEntity.status(404).body(responseDto));
    }

    private <T> Mono<ResponseEntity<ResponseDto<T>>> getInternalServerErrorResponse(Throwable error, String logMsg) {
        log.error(logMsg, error.getMessage(), error);
        ResponseDto<T> responseDto = new ResponseDto<>();
        responseDto.setStatus("BAD_REQUEST");
        responseDto.setMessage(error.getLocalizedMessage());
        responseDto.setStatusCode("400");
        return Mono.just(ResponseEntity.badRequest().body(responseDto));
    }
}