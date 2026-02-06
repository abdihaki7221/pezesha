package com.pezesha.cblms.repository;

import com.pezesha.cblms.models.Account;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * @author AOmar
 */
@Repository
public interface AccountsManagementRepository extends ReactiveCrudRepository<Account, Long> {


    Mono<Account> findByParentAccountId(Long parentAccountId);
    Mono<Account> findByCode(String accountCode);
    Mono<Account> findByCodeAndCurrency(String code, String currency);
}
