package com.edot.apm.pocket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PocketTransactionRepository extends JpaRepository<PocketTransaction, UUID> {

    List<PocketTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<PocketTransaction> findByIdAndUserId(UUID id, UUID userId);

    Optional<PocketTransaction> findByReferenceId(String referenceId);

    Optional<PocketTransaction> findByIdempotencyKey(String idempotencyKey);
}
