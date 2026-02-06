package com.pezesha.cblms.repository;

import com.pezesha.cblms.models.Borrower;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author AOmar
 */
public interface BorrowerRepository extends ReactiveCrudRepository<Borrower, Long> {
    Mono<Borrower> findByBorrowerId(String borrowerId);
}
