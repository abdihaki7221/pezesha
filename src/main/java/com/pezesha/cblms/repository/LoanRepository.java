package com.pezesha.cblms.repository;

import com.pezesha.cblms.models.Loan;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author AOmar
 */
public interface LoanRepository extends ReactiveCrudRepository<Loan, Long> {

    Mono<Loan> findByLoanId(String loanId);

    /**
     * Find active loans for aging report
     * Includes ACTIVE and DEFAULTED loans (excludes CLOSED, WRITTEN_OFF)
     */
    @Query("""
            SELECT * FROM tb_loans
            WHERE is_deleted = false
            AND status IN ('ACTIVE', 'DEFAULTED')
            AND (:currency IS NULL OR currency = :currency)
            AND (:status IS NULL OR status = :status)
            ORDER BY maturity_date
            """)
    Flux<Loan> findActiveLoans(
            @Param("currency") String currency,
            @Param("status") String status);


}