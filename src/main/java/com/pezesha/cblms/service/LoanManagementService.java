package com.pezesha.cblms.service;

import com.pezesha.cblms.dto.request.LoanDisbursementRequest;
import com.pezesha.cblms.dto.request.LoanRepaymentRequest;
import com.pezesha.cblms.dto.request.LoanWriteOffRequest;
import com.pezesha.cblms.dto.response.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Service interface for loan management operations
 *
 * @author AOmar
 */
public interface LoanManagementService {

    /**
     * Disburse a new loan
     *
     * @param request Loan disbursement request
     * @return ResponseEntity with LoanDisbursementResponse
     */
    Mono<ResponseEntity<ResponseDto<LoanDisbursementResponse>>> disburseLoan(LoanDisbursementRequest request);

    /**
     * Process loan repayment
     *
     * @param loanId Loan identifier
     * @param request Loan repayment request
     * @return ResponseEntity with LoanRepaymentResponse
     */
    Mono<ResponseEntity<ResponseDto<LoanRepaymentResponse>>> processRepayment(String loanId, LoanRepaymentRequest request);

    /**
     * Write off loan
     *
     * @param loanId Loan identifier
     * @param request Loan write-off request
     * @return ResponseEntity with LoanWriteOffResponse
     */
    Mono<ResponseEntity<ResponseDto<LoanWriteOffResponse>>> writeOffLoan(String loanId, LoanWriteOffRequest request);

    /**
     * Get loan details
     *
     * @param loanId Loan identifier
     * @return ResponseEntity with LoanDetailsResponse
     */
    Mono<ResponseEntity<ResponseDto<LoanDetailsResponse>>> getLoanDetails(String loanId);

    /**
     * List loans with filters and pagination
     *
     * @param borrowerId Optional borrower ID filter
     * @param status Optional loan status filter
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param page Page number
     * @param size Page size
     * @return ResponseEntity with LoanListResponse
     */
    Mono<ResponseEntity<ResponseDto<LoanListResponse>>> listLoans(
            String borrowerId, String status, Instant startDate, Instant endDate, Integer page, Integer size);
}