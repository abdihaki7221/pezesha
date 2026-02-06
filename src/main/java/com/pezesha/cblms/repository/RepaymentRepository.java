package com.pezesha.cblms.repository;

import com.pezesha.cblms.models.Repayment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for Repayment entity
 *
 * @author AOmar
 */
@Repository
public interface RepaymentRepository extends ReactiveCrudRepository<Repayment, Long> {

    /**
     * Find all repayments for a loan ordered by payment date descending
     *
     * @param loanId Loan identifier
     * @return Flux of Repayment
     */
    Flux<Repayment> findByLoanIdOrderByPaymentDateDesc(String loanId);

    /**
     * Find repayment by repayment ID
     *
     * @param repaymentId Repayment identifier
     * @return Mono of Repayment
     */
    Mono<Repayment> findByRepaymentId(String repaymentId);

    /**
     * Find all repayments for a loan
     *
     * @param loanId Loan identifier
     * @return Flux of Repayment
     */
    Flux<Repayment> findByLoanId(String loanId);
}