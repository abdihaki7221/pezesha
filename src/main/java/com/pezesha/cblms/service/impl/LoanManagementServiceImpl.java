package com.pezesha.cblms.service.impl;

import com.pezesha.cblms.dto.request.CreateTransactionRequest;
import com.pezesha.cblms.dto.request.JournalEntryRequest;
import com.pezesha.cblms.dto.request.LoanDisbursementRequest;
import com.pezesha.cblms.dto.request.LoanRepaymentRequest;
import com.pezesha.cblms.dto.request.LoanWriteOffRequest;
import com.pezesha.cblms.dto.response.*;
import com.pezesha.cblms.enums.AllocationStrategy;
import com.pezesha.cblms.enums.LoanStatus;
import com.pezesha.cblms.enums.WriteOffType;
import com.pezesha.cblms.exceptions.ServiceValidation;
import com.pezesha.cblms.models.*;
import com.pezesha.cblms.repository.*;
import com.pezesha.cblms.service.LoanManagementService;
import com.pezesha.cblms.service.TransactionManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of LoanManagementService
 * Handles all loan operations including disbursement, repayment, write-off, and queries
 *
 * @author AOmar
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoanManagementServiceImpl implements LoanManagementService {

    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final WriteOffRepository writeOffRepository;
    private final AccountsManagementRepository accountsManagementRepository;
    private final TransactionManagementService transactionManagementService;
    private final IdempotencyRepository idempotencyRepository;
    private final BorrowerRepository borrowerRepository;

    private static final String LOANS_REC_CODE = "10001";

    private static final String FEE_INC_CODE = "40001";
    private static final String INT_INC_CODE = "40002";
    private static final String BAD_DEBT_EXP_CODE = "50001";


    @Override
    @Transactional
    public Mono<ResponseEntity<ResponseDto<LoanDisbursementResponse>>> disburseLoan(LoanDisbursementRequest request) {
        log.info("Processing loan disbursement request for borrower: {}", request.getBorrowerId());

        return idempotencyRepository.findByKey(request.getIdempotencyKey())
                .flatMap(res -> processDuplicateIdempotencyKey())
                .switchIfEmpty(processLoanDisbursement(request))
                .onErrorResume(err -> {
                    log.error("Error processing loan disbursement: {}", err.getMessage(), err);
                    ResponseDto<LoanDisbursementResponse> responseDto = new ResponseDto<>();
                    responseDto.setMessage(err.getMessage());
                    responseDto.setStatusCode("400");
                    responseDto.setStatus("BAD_REQUEST");
                    return Mono.just(ResponseEntity.badRequest().body(responseDto));
                });
    }

    private Mono<ResponseEntity<ResponseDto<LoanDisbursementResponse>>> processDuplicateIdempotencyKey() {
        ResponseDto<LoanDisbursementResponse> responseDto = new ResponseDto<>();
        responseDto.setMessage("Duplicate idempotency key");
        responseDto.setStatusCode("409");
        responseDto.setStatus("CONFLICT");
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(responseDto));
    }

    private Mono<ResponseEntity<ResponseDto<LoanDisbursementResponse>>> processLoanDisbursement(LoanDisbursementRequest request) {
        if (!request.getMaturityDate().isAfter(request.getDisbursementDate())) {
            return processInvalidLoanTerm();
        }

        BigDecimal netDisbursement = request.getPrincipalAmount().subtract(request.getOriginationFee());

        return accountsManagementRepository.findByCodeAndCurrency(request.getDisbursementAccountId(), request.getCurrency())
                .switchIfEmpty(Mono.error(new ServiceValidation("Disbursement account not found")))
                .flatMap(disbAccount -> validateAccount(disbAccount, "ASSET", request.getCurrency())
                        .then(checkSufficientBalance(disbAccount, netDisbursement))
                        .then(accountsManagementRepository.findByCodeAndCurrency(LOANS_REC_CODE, request.getCurrency()))
                        .switchIfEmpty(Mono.error(new ServiceValidation("Loans receivable account not found")))
                        .flatMap(loansRec -> validateAccount(loansRec, "ASSET", request.getCurrency()))
                        .then(accountsManagementRepository.findByCodeAndCurrency(FEE_INC_CODE, request.getCurrency()))
                        .switchIfEmpty(Mono.error(new ServiceValidation("Fee income account not found")))
                        .flatMap(feeInc -> validateAccount(feeInc, "INCOME", request.getCurrency()))
                        .then(Mono.just(disbAccount)))
                .flatMap(disbAccount -> {
                    String loanIdStr = "loan_" + UUID.randomUUID().toString().substring(0, 8);

                    Borrower borrower = new Borrower();
                    borrower.setBorrowerId(request.getBorrowerId());
                    borrower.setBorrowerName(request.getBorrowerName());
                    borrower.setDeleted(false);
                    borrower.setCreatedAt(LocalDateTime.now());

                    borrowerRepository.save(borrower).subscribe();

                    Loan loan = Loan.builder()
                            .loanId(loanIdStr)
                            .borrowerId(request.getBorrowerId())
                            .principalAmount(request.getPrincipalAmount())
                            .interestRate(request.getInterestRate())
                            .originationFee(request.getOriginationFee())
                            .currency(request.getCurrency())
                            .disbursementDate(request.getDisbursementDate())
                            .maturityDate(request.getMaturityDate())
                            .status(LoanStatus.ACTIVE.name())
                            .outstandingPrincipal(request.getPrincipalAmount())
                            .outstandingInterest(BigDecimal.ZERO)
                            .totalPaid(BigDecimal.ZERO)
                            .createdAt(LocalDateTime.now())
                            .build();

                    return accountsManagementRepository.findByCode(LOANS_REC_CODE)
                            .flatMap(acc -> {
                                return loanRepository.save(loan)
                                        .flatMap(savedLoan -> createDisbursementTransaction(request, savedLoan, disbAccount,acc.getId())
                                                .flatMap(disbTxResp -> createFeeTransaction(request, savedLoan, disbAccount)
                                                        .flatMap(feeTxResp -> saveIdempotency(request.getIdempotencyKey(), savedLoan.getId())
                                                                .then(buildDisbursementResponse(savedLoan)))));
                            }) .map(response -> {
                                ResponseDto<LoanDisbursementResponse> dto = new ResponseDto<>();
                                dto.setStatus("CREATED");
                                dto.setStatusCode("201");
                                dto.setMessage("Loan disbursed successfully");
                                dto.setBody(response);
                                log.info("Loan disbursed successfully: {}", response.getLoanId());
                                return ResponseEntity.status(HttpStatus.CREATED).body(dto);
                            });
                });


    }

    private Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> createDisbursementTransaction(
            LoanDisbursementRequest request, Loan loan, Account disbAccount, Long id) {

        CreateTransactionRequest txReq = CreateTransactionRequest.builder()
                .idempotencyKey(request.getIdempotencyKey() + "_disb")
                .transactionDate(request.getDisbursementDate())
                .description("Loan disbursement for " + loan.getLoanId())
                .currency(request.getCurrency())
                .entries(List.of(
                        JournalEntryRequest.builder()
                                .accountId(String.valueOf(id))
                                .debit(request.getPrincipalAmount())
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalEntryRequest.builder()
                                .accountId(String.valueOf(disbAccount.getId()))
                                .debit(BigDecimal.ZERO)
                                .credit(request.getPrincipalAmount())
                                .build()
                ))
                .build();
        return transactionManagementService.createTransaction(txReq);
    }

    private Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> createFeeTransaction(
            LoanDisbursementRequest request, Loan loan, Account disbAccount) {
        CreateTransactionRequest txReq = CreateTransactionRequest.builder()
                .idempotencyKey(request.getIdempotencyKey() + "_fee")
                .transactionDate(request.getDisbursementDate())
                .description("Origination fee for " + loan.getLoanId())
                .currency(request.getCurrency())
                .entries(List.of(
                        JournalEntryRequest.builder()
                                .accountId(disbAccount.getCode())
                                .debit(request.getOriginationFee())
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalEntryRequest.builder()
                                .accountId(FEE_INC_CODE)
                                .debit(BigDecimal.ZERO)
                                .credit(request.getOriginationFee())
                                .build()
                ))
                .build();
        return transactionManagementService.createTransaction(txReq);
    }

    private Mono<LoanDisbursementResponse> buildDisbursementResponse(Loan loan) {


        return Mono.just(LoanDisbursementResponse.builder()
                .loanId(loan.getLoanId())
                .borrowerId(loan.getBorrowerId())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .originationFee(loan.getOriginationFee())
                .netDisbursement(loan.getPrincipalAmount().subtract(loan.getOriginationFee()))
                .currency(loan.getCurrency())
                .disbursementDate(loan.getDisbursementDate())
                .maturityDate(loan.getMaturityDate())
                .status(loan.getStatus())
                .outstandingPrincipal(loan.getOutstandingPrincipal())
                .outstandingInterest(loan.getOutstandingInterest())
                .build());
    }


    @Override
    @Transactional
    public Mono<ResponseEntity<ResponseDto<LoanRepaymentResponse>>> processRepayment(
            String loanId, LoanRepaymentRequest request) {
        log.info("Processing loan repayment for loan: {}, amount: {}", loanId, request.getAmount());

        return idempotencyRepository.findByKey(request.getIdempotencyKey())
                .flatMap(res -> processDuplicateRepaymentIdempotencyKey())
                .switchIfEmpty(processLoanRepayment(loanId, request))
                .onErrorResume(err -> {
                    log.error("Error processing loan repayment: {}", err.getMessage(), err);
                    ResponseDto<LoanRepaymentResponse> responseDto = new ResponseDto<>();
                    responseDto.setMessage(err.getMessage());
                    responseDto.setStatusCode("400");
                    responseDto.setStatus("BAD_REQUEST");
                    return Mono.just(ResponseEntity.badRequest().body(responseDto));
                });
    }

    private Mono<ResponseEntity<ResponseDto<LoanRepaymentResponse>>> processDuplicateRepaymentIdempotencyKey() {
        ResponseDto<LoanRepaymentResponse> responseDto = new ResponseDto<>();
        responseDto.setMessage("Duplicate idempotency key");
        responseDto.setStatusCode("409");
        responseDto.setStatus("CONFLICT");
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(responseDto));
    }

    private Mono<ResponseEntity<ResponseDto<LoanRepaymentResponse>>> processLoanRepayment(
            String loanId, LoanRepaymentRequest request) {

        return loanRepository.findByLoanId(loanId)
                .switchIfEmpty(Mono.error(new ServiceValidation("Loan '" + loanId + "' not found")))
                .flatMap(loan -> {
                    if (!loan.getStatus().equals(LoanStatus.ACTIVE.name())) {
                        return Mono.error(new ServiceValidation(
                                "Cannot process payment for loan with status: " + loan.getStatus()));
                    }

                    BigDecimal totalOutstanding = loan.getOutstandingPrincipal()
                            .add(loan.getOutstandingInterest());

                    if (request.getAmount().compareTo(totalOutstanding) > 0) {
                        return Mono.error(new ServiceValidation(
                                "Payment amount (" + request.getAmount() +
                                        ") exceeds total outstanding balance (" + totalOutstanding + ")"));
                    }

                    return accountsManagementRepository.findByCodeAndCurrency(
                                    request.getPaymentAccountId(), loan.getCurrency())
                            .switchIfEmpty(Mono.error(new ServiceValidation("Payment account not found")))
                            .flatMap(paymentAccount -> validateAccount(paymentAccount, "ASSET", loan.getCurrency()))
                            .then(Mono.just(loan));
                })
                .flatMap(loan -> {
                    BigDecimal accruedInterest = calculateAccruedInterest(
                            loan.getPrincipalAmount(),
                            loan.getInterestRate(),
                            loan.getDisbursementDate(),
                            request.getPaymentDate()
                    );

                    loan.setOutstandingInterest(accruedInterest);

                    PaymentAllocation allocation = allocatePayment(
                            request.getAmount(),
                            loan.getOutstandingPrincipal(),
                            loan.getOutstandingInterest(),
                            request.getAllocationStrategy()
                    );

                    loan.setOutstandingPrincipal(
                            loan.getOutstandingPrincipal().subtract(allocation.getPrincipal()));
                    loan.setOutstandingInterest(
                            loan.getOutstandingInterest().subtract(allocation.getInterest()));
                    loan.setTotalPaid(
                            (loan.getTotalPaid() != null ? loan.getTotalPaid() : BigDecimal.ZERO)
                                    .add(request.getAmount()));
                    loan.setLastPaymentDate(request.getPaymentDate());

                    if (loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) == 0 &&
                            loan.getOutstandingInterest().compareTo(BigDecimal.ZERO) == 0) {
                        loan.setStatus(LoanStatus.CLOSED.name());
                        loan.setClosureDate(request.getPaymentDate());
                    }

                    String repaymentIdStr = "repay_" + UUID.randomUUID().toString().substring(0, 8);
                    Repayment repayment = Repayment.builder()
                            .repaymentId(repaymentIdStr)
                            .loanId(loan.getLoanId())
                            .amount(request.getAmount())
                            .paymentDate(request.getPaymentDate())
                            .principalPaid(allocation.getPrincipal())
                            .interestPaid(allocation.getInterest())
                            .allocationStrategy(request.getAllocationStrategy().name())
                            .paymentAccountId(request.getPaymentAccountId())
                            .createdAt(LocalDateTime.now())
                            .build();

                    return repaymentRepository.save(repayment)
                            .flatMap(savedRepayment -> {
                                return createRepaymentTransaction(request, loan, allocation, savedRepayment)
                                        .flatMap(txResp -> repaymentRepository.save(savedRepayment))
                                        .then(loanRepository.save(loan))
                                        .then(saveIdempotency(request.getIdempotencyKey(), savedRepayment.getId()))
                                        .then(buildRepaymentResponse(loan, savedRepayment, allocation));
                            });
                })
                .map(response -> {
                    ResponseDto<LoanRepaymentResponse> dto = new ResponseDto<>();
                    dto.setStatus("CREATED");
                    dto.setStatusCode("201");
                    dto.setMessage("Loan repayment processed successfully");
                    dto.setBody(response);
                    log.info("Loan repayment processed successfully for loan: {}", loanId);
                    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
                });
    }

    private Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> createRepaymentTransaction(
            LoanRepaymentRequest request, Loan loan, PaymentAllocation allocation, Repayment repayment) {

        CreateTransactionRequest txReq = CreateTransactionRequest.builder()
                .idempotencyKey(request.getIdempotencyKey() + "_txn")
                .transactionDate(request.getPaymentDate())
                .description("Loan repayment for " + loan.getLoanId())
                .currency(loan.getCurrency())
                .entries(List.of(
                        JournalEntryRequest.builder()
                                .accountId(request.getPaymentAccountId())
                                .debit(request.getAmount())
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalEntryRequest.builder()
                                .accountId(LOANS_REC_CODE)
                                .debit(BigDecimal.ZERO)
                                .credit(allocation.getPrincipal())
                                .build(),
                        JournalEntryRequest.builder()
                                .accountId(INT_INC_CODE)
                                .debit(BigDecimal.ZERO)
                                .credit(allocation.getInterest())
                                .build()
                ))
                .build();

        return transactionManagementService.createTransaction(txReq);
    }

    private Mono<LoanRepaymentResponse> buildRepaymentResponse(
            Loan loan, Repayment repayment, PaymentAllocation allocation) {
        return Mono.just(LoanRepaymentResponse.builder()
                .repaymentId(repayment.getRepaymentId())
                .loanId(loan.getLoanId())
                .amount(repayment.getAmount())
                .paymentDate(repayment.getPaymentDate())
                .allocation(LoanRepaymentResponse.Allocation.builder()
                        .principal(allocation.getPrincipal())
                        .interest(allocation.getInterest())
                        .build())
                .loanBalance(LoanRepaymentResponse.LoanBalance.builder()
                        .outstandingPrincipal(loan.getOutstandingPrincipal())
                        .outstandingInterest(loan.getOutstandingInterest())
                        .totalOutstanding(loan.getOutstandingPrincipal().add(loan.getOutstandingInterest()))
                        .build())
                .status("POSTED")
                .build());
    }


    @Override
    @Transactional
    public Mono<ResponseEntity<ResponseDto<LoanWriteOffResponse>>> writeOffLoan(
            String loanId, LoanWriteOffRequest request) {
        log.info("Processing loan write-off for loan: {}, type: {}", loanId, request.getWriteOffType());

        return idempotencyRepository.findByKey(request.getIdempotencyKey())
                .flatMap(res -> processDuplicateWriteOffIdempotencyKey())
                .switchIfEmpty(processLoanWriteOff(loanId, request))
                .onErrorResume(err -> {
                    log.error("Error processing loan write-off: {}", err.getMessage(), err);
                    ResponseDto<LoanWriteOffResponse> responseDto = new ResponseDto<>();
                    responseDto.setMessage(err.getMessage());
                    responseDto.setStatusCode("400");
                    responseDto.setStatus("BAD_REQUEST");
                    return Mono.just(ResponseEntity.badRequest().body(responseDto));
                });
    }

    private Mono<ResponseEntity<ResponseDto<LoanWriteOffResponse>>> processDuplicateWriteOffIdempotencyKey() {
        ResponseDto<LoanWriteOffResponse> responseDto = new ResponseDto<>();
        responseDto.setMessage("Duplicate idempotency key");
        responseDto.setStatusCode("409");
        responseDto.setStatus("CONFLICT");
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(responseDto));
    }

    private Mono<ResponseEntity<ResponseDto<LoanWriteOffResponse>>> processLoanWriteOff(
            String loanId, LoanWriteOffRequest request) {

        return loanRepository.findByLoanId(loanId)
                .switchIfEmpty(Mono.error(new ServiceValidation("Loan '" + loanId + "' not found")))
                .flatMap(loan -> {
                    if (!loan.getStatus().equals(LoanStatus.ACTIVE.name()) &&
                            !loan.getStatus().equals(LoanStatus.DEFAULTED.name())) {
                        return Mono.error(new ServiceValidation(
                                "Cannot write off loan with status: " + loan.getStatus()));
                    }

                    BigDecimal totalOutstanding = loan.getOutstandingPrincipal()
                            .add(loan.getOutstandingInterest());

                    if (totalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
                        return Mono.error(new ServiceValidation(
                                "Loan has no outstanding balance to write off"));
                    }

                    if (request.getWriteOffDate().isBefore(loan.getDisbursementDate())) {
                        return Mono.error(new ServiceValidation(
                                "Write-off date cannot be before loan disbursement date"));
                    }

                    WriteOffAmounts amounts;
                    if (request.getWriteOffType() == WriteOffType.FULL) {
                        amounts = new WriteOffAmounts(
                                loan.getOutstandingPrincipal(),
                                loan.getOutstandingInterest(),
                                totalOutstanding
                        );
                    } else {
                        if (request.getPartialAmount() == null) {
                            return Mono.error(new ServiceValidation(
                                    "Partial amount is required for PARTIAL write-off"));
                        }
                        if (request.getPartialAmount().compareTo(totalOutstanding) > 0) {
                            return Mono.error(new ServiceValidation(
                                    "Partial write-off amount (" + request.getPartialAmount() +
                                            ") exceeds total outstanding (" + totalOutstanding + ")"));
                        }

                        BigDecimal principalRatio = loan.getOutstandingPrincipal()
                                .divide(totalOutstanding, 4, RoundingMode.HALF_UP);
                        BigDecimal principalWriteOff = request.getPartialAmount()
                                .multiply(principalRatio)
                                .setScale(2, RoundingMode.HALF_UP);
                        BigDecimal interestWriteOff = request.getPartialAmount()
                                .subtract(principalWriteOff);

                        amounts = new WriteOffAmounts(
                                principalWriteOff,
                                interestWriteOff,
                                request.getPartialAmount()
                        );
                    }

                    loan.setOutstandingPrincipal(
                            loan.getOutstandingPrincipal().subtract(amounts.principal));
                    loan.setOutstandingInterest(
                            loan.getOutstandingInterest().subtract(amounts.interest));
                    loan.setStatus(LoanStatus.WRITTEN_OFF.name());

                    String writeOffIdStr = "writeoff_" + UUID.randomUUID().toString().substring(0, 8);
                    WriteOff writeOff = WriteOff.builder()
                            .writeOffId(writeOffIdStr)
                            .loanId(loan.getLoanId())
                            .writeOffDate(request.getWriteOffDate())
                            .writeOffType(request.getWriteOffType().name())
                            .principalWrittenOff(amounts.principal)
                            .interestWrittenOff(amounts.interest)
                            .totalWrittenOff(amounts.total)
                            .reason(request.getReason())
                            .createdAt(LocalDateTime.now())
                            .build();

                    return writeOffRepository.save(writeOff)
                            .flatMap(savedWriteOff -> createWriteOffTransaction(request, loan, amounts, savedWriteOff)
                                    .flatMap(txResp -> {
                                        return writeOffRepository.save(savedWriteOff);
                                    })
                                    .then(loanRepository.save(loan))
                                    .then(saveIdempotency(request.getIdempotencyKey(), savedWriteOff.getId()))
                                    .then(buildWriteOffResponse(loan, savedWriteOff, amounts)));
                })
                .map(response -> {
                    ResponseDto<LoanWriteOffResponse> dto = new ResponseDto<>();
                    dto.setStatus("CREATED");
                    dto.setStatusCode("201");
                    dto.setMessage("Loan write-off processed successfully");
                    dto.setBody(response);
                    log.info("Loan write-off processed successfully for loan: {}", loanId);
                    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
                });
    }

    private Mono<ResponseEntity<ResponseDto<TransactionDetailsResponse>>> createWriteOffTransaction(
            LoanWriteOffRequest request, Loan loan, WriteOffAmounts amounts, WriteOff writeOff) {

        CreateTransactionRequest txReq = CreateTransactionRequest.builder()
                .idempotencyKey(request.getIdempotencyKey() + "_txn")
                .transactionDate(request.getWriteOffDate())
                .description("Loan write-off for " + loan.getLoanId() + " - " + request.getReason())
                .currency(loan.getCurrency())
                .entries(List.of(
                        JournalEntryRequest.builder()
                                .accountId(BAD_DEBT_EXP_CODE)
                                .debit(amounts.total)
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalEntryRequest.builder()
                                .accountId(LOANS_REC_CODE)
                                .debit(BigDecimal.ZERO)
                                .credit(amounts.principal)
                                .build(),
                        JournalEntryRequest.builder()
                                .accountId(INT_INC_CODE)
                                .debit(BigDecimal.ZERO)
                                .credit(amounts.interest)
                                .build()
                ))
                .build();

        return transactionManagementService.createTransaction(txReq);
    }

    private Mono<LoanWriteOffResponse> buildWriteOffResponse(
            Loan loan, WriteOff writeOff, WriteOffAmounts amounts) {
        return Mono.just(LoanWriteOffResponse.builder()
                .writeOffId(writeOff.getWriteOffId())
                .loanId(loan.getLoanId())
                .writeOffDate(writeOff.getWriteOffDate())
                .writeOffType(String.valueOf(WriteOffType.valueOf(writeOff.getWriteOffType())))
                .amountWrittenOff(LoanWriteOffResponse.AmountWrittenOff.builder()
                        .principal(amounts.principal)
                        .interest(amounts.interest)
                        .total(amounts.total)
                        .build())
                .reason(writeOff.getReason())
                .loanStatus(loan.getStatus())
                .build());
    }

    @Override
    public Mono<ResponseEntity<ResponseDto<LoanDetailsResponse>>> getLoanDetails(String loanId) {
        log.info("Fetching loan details for loan: {}", loanId);

        return loanRepository.findByLoanId(loanId)
                .switchIfEmpty(Mono.error(new ServiceValidation("Loan '" + loanId + "' not found")))
                .flatMap(loan -> {
                    long daysOverdue = 0;
                    if (loan.getMaturityDate().isBefore(Instant.now())) {
                        daysOverdue = ChronoUnit.DAYS.between(loan.getMaturityDate(), Instant.now());
                    }

                    final long finalDaysOverdue = daysOverdue;

                    return repaymentRepository.findByLoanIdOrderByPaymentDateDesc(loan.getLoanId())
                            .map(repayment -> LoanDetailsResponse.RepaymentHistoryItem.builder()
                                    .repaymentId(repayment.getRepaymentId())
                                    .amount(repayment.getAmount())
                                    .date(repayment.getPaymentDate())
                                    .principalPaid(repayment.getPrincipalPaid())
                                    .interestPaid(repayment.getInterestPaid())
                                    .build())
                            .collectList()
                            .map(repaymentHistory -> LoanDetailsResponse.builder()
                                    .loanId(loan.getLoanId())
                                    .borrowerId(loan.getBorrowerId())
                                    .principalAmount(loan.getPrincipalAmount())
                                    .interestRate(loan.getInterestRate())
                                    .originationFee(loan.getOriginationFee())
                                    .currency(loan.getCurrency())
                                    .disbursementDate(loan.getDisbursementDate())
                                    .maturityDate(loan.getMaturityDate())
                                    .status(loan.getStatus())
                                    .outstandingPrincipal(loan.getOutstandingPrincipal())
                                    .outstandingInterest(loan.getOutstandingInterest())
                                    .totalOutstanding(loan.getOutstandingPrincipal().add(loan.getOutstandingInterest()))
                                    .totalPaid(loan.getTotalPaid() != null ? loan.getTotalPaid() : BigDecimal.ZERO)
                                    .daysOverdue(finalDaysOverdue)
                                    .lastPaymentDate(loan.getLastPaymentDate())
                                    .repaymentHistory(repaymentHistory)
                                    .build());
                })
                .map(response -> {
                    ResponseDto<LoanDetailsResponse> dto = new ResponseDto<>();
                    dto.setStatus("OK");
                    dto.setStatusCode("200");
                    dto.setMessage("Loan details retrieved successfully");
                    dto.setBody(response);
                    return ResponseEntity.ok(dto);
                })
                .onErrorResume(err -> {
                    log.error("Error fetching loan details: {}", err.getMessage(), err);
                    ResponseDto<LoanDetailsResponse> responseDto = new ResponseDto<>();
                    responseDto.setMessage(err.getMessage());
                    responseDto.setStatusCode("404");
                    responseDto.setStatus("NOT_FOUND");
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto));
                });
    }


    @Override
    public Mono<ResponseEntity<ResponseDto<LoanListResponse>>> listLoans(
            String borrowerId, String status, Instant startDate, Instant endDate, Integer page, Integer size) {
        log.info("Listing loans with filters - borrowerId: {}, status: {}, page: {}, size: {}",
                borrowerId, status, page, size);

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            ResponseDto<LoanListResponse> responseDto = new ResponseDto<>();
            responseDto.setMessage("End date must be after start date");
            responseDto.setStatusCode("400");
            responseDto.setStatus("BAD_REQUEST");
            return Mono.just(ResponseEntity.badRequest().body(responseDto));
        }

        Flux<Loan> loansFlux = loanRepository.findAll();

        if (borrowerId != null && !borrowerId.isEmpty()) {
            loansFlux = loansFlux.filter(loan -> loan.getBorrowerId().equals(borrowerId));
        }

        if (status != null && !status.isEmpty()) {
            loansFlux = loansFlux.filter(loan -> loan.getStatus().equals(status));
        }

        if (startDate != null) {
            loansFlux = loansFlux.filter(loan ->
                    loan.getDisbursementDate().isAfter(startDate) ||
                            loan.getDisbursementDate().equals(startDate));
        }

        if (endDate != null) {
            loansFlux = loansFlux.filter(loan ->
                    loan.getDisbursementDate().isBefore(endDate) ||
                            loan.getDisbursementDate().equals(endDate));
        }

        return loansFlux.collectList()
                .flatMap(allLoans -> {
                    long totalElements = allLoans.size();
                    long totalPages = (totalElements + size - 1) / size;

                    int skip = page * size;
                    List<Loan> pagedLoans = allLoans.stream()
                            .skip(skip)
                            .limit(size)
                            .toList();

                    return Flux.fromIterable(pagedLoans)
                            .flatMap(loan -> {
                                long daysOverdue = 0;
                                if (loan.getMaturityDate().isBefore(Instant.now())) {
                                    daysOverdue = ChronoUnit.DAYS.between(loan.getMaturityDate(), Instant.now());
                                }

                                return Mono.just(LoanListResponse.LoanSummary.builder()
                                        .loanId(loan.getLoanId())
                                        .borrowerId(loan.getBorrowerId())
                                        .principalAmount(loan.getPrincipalAmount())
                                        .outstandingPrincipal(loan.getOutstandingPrincipal())
                                        .outstandingInterest(loan.getOutstandingInterest())
                                        .status(loan.getStatus())
                                        .disbursementDate(loan.getDisbursementDate())
                                        .maturityDate(loan.getMaturityDate())
                                        .daysOverdue(daysOverdue)
                                        .build());
                            })
                            .collectList()
                            .map(loanSummaries -> LoanListResponse.builder()
                                    .loans(loanSummaries)
                                    .pagination(LoanListResponse.Pagination.builder()
                                            .currentPage(page)
                                            .pageSize(size)
                                            .totalPages(totalPages)
                                            .totalElements(totalElements)
                                            .build())
                                    .build());
                })
                .map(response -> {
                    ResponseDto<LoanListResponse> dto = new ResponseDto<>();
                    dto.setStatus("OK");
                    dto.setStatusCode("200");
                    dto.setMessage("Loans retrieved successfully");
                    dto.setBody(response);
                    return ResponseEntity.ok(dto);
                })
                .onErrorResume(err -> {
                    log.error("Error listing loans: {}", err.getMessage(), err);
                    ResponseDto<LoanListResponse> responseDto = new ResponseDto<>();
                    responseDto.setMessage(err.getMessage());
                    responseDto.setStatusCode("500");
                    responseDto.setStatus("INTERNAL_SERVER_ERROR");
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDto));
                });
    }

    private Mono<Account> validateAccount(Account account, String expectedType, String currency) {
        if (!account.getType().equals(expectedType)) {
            return Mono.error(new ServiceValidation(
                    "Account must be " + expectedType + " type, found: " + account.getType()));
        }
        if (!account.getCurrency().equals(currency)) {
            return Mono.error(new ServiceValidation(
                    "Currency mismatch for account " + account.getCode()));
        }
        return Mono.just(account);
    }

    private Mono<Void> checkSufficientBalance(Account account, BigDecimal required) {
        if (account.getCurrentBalance().compareTo(required) < 0) {
            return Mono.error(new ServiceValidation(
                    "Account '" + account.getCode() + "' has insufficient balance. Available: " +
                            account.getCurrentBalance() + ", Required: " + required));
        }
        return Mono.empty();
    }

    private Mono<Void> saveIdempotency(String key, Long refId) {
        Idempotency imp = Idempotency.builder()
                .key(key)
                .transactionId(refId)
                .createdAt(Instant.now())
                .build();
        return idempotencyRepository.save(imp).then();
    }

    private Mono<ResponseEntity<ResponseDto<LoanDisbursementResponse>>> processInvalidLoanTerm() {
        ResponseDto<LoanDisbursementResponse> dto = new ResponseDto<>();
        dto.setStatus("BAD_REQUEST");
        dto.setStatusCode("400");
        dto.setMessage("Maturity date must be after disbursement date");
        return Mono.just(ResponseEntity.badRequest().body(dto));
    }


    private BigDecimal calculateAccruedInterest(
            BigDecimal principal, BigDecimal interestRate, Instant startDate, Instant endDate) {

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal dailyRate = interestRate
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP);

        BigDecimal interest = principal
                .multiply(dailyRate)
                .multiply(BigDecimal.valueOf(daysBetween))
                .setScale(2, RoundingMode.HALF_UP);

        return interest;
    }

    private PaymentAllocation allocatePayment(
            BigDecimal amount, BigDecimal outstandingPrincipal,
            BigDecimal outstandingInterest, AllocationStrategy strategy) {

        BigDecimal principalPaid;
        BigDecimal interestPaid;

        switch (strategy) {
            case INTEREST_FIRST:
                interestPaid = amount.min(outstandingInterest);
                principalPaid = amount.subtract(interestPaid).min(outstandingPrincipal);
                break;

            case PRINCIPAL_FIRST:
                principalPaid = amount.min(outstandingPrincipal);
                interestPaid = amount.subtract(principalPaid).min(outstandingInterest);
                break;

            case PROPORTIONAL:
                BigDecimal totalOutstanding = outstandingPrincipal.add(outstandingInterest);
                if (totalOutstanding.compareTo(BigDecimal.ZERO) == 0) {
                    principalPaid = BigDecimal.ZERO;
                    interestPaid = BigDecimal.ZERO;
                } else {
                    BigDecimal principalRatio = outstandingPrincipal
                            .divide(totalOutstanding, 4, RoundingMode.HALF_UP);
                    principalPaid = amount.multiply(principalRatio)
                            .setScale(2, RoundingMode.HALF_UP)
                            .min(outstandingPrincipal);
                    interestPaid = amount.subtract(principalPaid).min(outstandingInterest);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown allocation strategy: " + strategy);
        }

        return new PaymentAllocation(principalPaid, interestPaid);
    }

    private static class PaymentAllocation {
        private final BigDecimal principal;
        private final BigDecimal interest;

        public PaymentAllocation(BigDecimal principal, BigDecimal interest) {
            this.principal = principal;
            this.interest = interest;
        }

        public BigDecimal getPrincipal() {
            return principal;
        }

        public BigDecimal getInterest() {
            return interest;
        }
    }

    private static class WriteOffAmounts {
        private final BigDecimal principal;
        private final BigDecimal interest;
        private final BigDecimal total;

        public WriteOffAmounts(BigDecimal principal, BigDecimal interest, BigDecimal total) {
            this.principal = principal;
            this.interest = interest;
            this.total = total;
        }
    }
}