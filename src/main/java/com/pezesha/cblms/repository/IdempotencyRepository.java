package com.pezesha.cblms.repository;

import com.pezesha.cblms.models.Idempotency;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author AOmar
 */
public interface IdempotencyRepository extends ReactiveCrudRepository<Idempotency, Long> {

    Mono<Idempotency> findByKey(String key);
}