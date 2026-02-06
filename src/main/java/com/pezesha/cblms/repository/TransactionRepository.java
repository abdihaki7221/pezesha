package com.pezesha.cblms.repository;

import com.pezesha.cblms.models.Transaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author AOmar
 */
public interface TransactionRepository extends ReactiveCrudRepository<Transaction, Long> {

    Mono<Transaction> findByTransactionId(String transactionId);
}