package com.pezesha.cblms.repository;

import com.pezesha.cblms.models.WriteOff;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for WriteOff entity
 *
 * @author AOmar
 */
@Repository
public interface WriteOffRepository extends ReactiveCrudRepository<WriteOff, Long> {

    /**
     * Find write-off by loan ID
     *
     * @param loanId Loan identifier
     * @return Mono of WriteOff
     */
    Mono<WriteOff> findByLoanId(String loanId);

    /**
     * Find write-off by write-off ID
     *
     * @param writeOffId Write-off identifier
     * @return Mono of WriteOff
     */
    Mono<WriteOff> findByWriteOffId(String writeOffId);
}