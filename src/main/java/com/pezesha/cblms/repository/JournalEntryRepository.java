package com.pezesha.cblms.repository;

import com.pezesha.cblms.models.JournalEntry;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * @author AOmar
 */
public interface JournalEntryRepository extends ReactiveCrudRepository<JournalEntry, Long> {

    Flux<JournalEntry> findByTransactionId(Long transactionId);

    Flux<JournalEntry> findByAccountId(Long accountId);

    Mono<Long> countByTransactionId(Long transactionId);
    /**
     * Find all journal entries up to a specific date
     * Used for Trial Balance and Balance Sheet reports
     */
    @Query("""
            SELECT je.* FROM tb_journal_entries je
            INNER JOIN tb_transactions t ON je.transaction_id = t.id
            WHERE t.transaction_date <= :asOfDate
            AND (:currency IS NULL OR t.currency = :currency)
            AND t.status = 'POSTED'
            ORDER BY t.transaction_date, je.id
            """)
    Flux<JournalEntry> findAllEntriesUpToDate(
            @Param("asOfDate") Instant asOfDate,
            @Param("currency") String currency);

    /**
     * Find journal entries between two dates
     * Used for Income Statement report
     */
    @Query("""
            SELECT je.* FROM tb_journal_entries je
            INNER JOIN tb_transactions t ON je.transaction_id = t.id
            WHERE t.transaction_date >= :startDate
            AND t.transaction_date <= :endDate
            AND (:currency IS NULL OR t.currency = :currency)
            AND t.status = 'POSTED'
            ORDER BY t.transaction_date, je.id
            """)
    Flux<JournalEntry> findEntriesBetweenDates(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("currency") String currency);

    /**
     * Find entries for a specific account type up to a date
     */
    @Query("""
            SELECT je.* FROM tb_journal_entries je
            INNER JOIN tb_transactions t ON je.transaction_id = t.id
            WHERE je.account_type = :accountType
            AND t.transaction_date <= :asOfDate
            AND (:currency IS NULL OR t.currency = :currency)
            AND t.status = 'POSTED'
            ORDER BY t.transaction_date
            """)
    Flux<JournalEntry> findByAccountTypeAndDateUpTo(
            @Param("accountType") String accountType,
            @Param("asOfDate") Instant asOfDate,
            @Param("currency") String currency);

    /**
     * Find entries for a specific account type between dates
     */
    @Query("""
            SELECT je.* FROM tb_journal_entries je
            INNER JOIN tb_transactions t ON je.transaction_id = t.id
            WHERE je.account_type = :accountType
            AND t.transaction_date >= :startDate
            AND t.transaction_date <= :endDate
            AND (:currency IS NULL OR t.currency = :currency)
            AND t.status = 'POSTED'
            ORDER BY t.transaction_date
            """)
    Flux<JournalEntry> findByAccountTypeAndDateBetween(
            @Param("accountType") String accountType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("currency") String currency);
}