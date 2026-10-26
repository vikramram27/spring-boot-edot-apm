package com.edot.apm.pocket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PocketRepository extends JpaRepository<Pocket, UUID> {

    Optional<Pocket> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
