package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.domain.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperationTypeRepository extends JpaRepository<OperationType, Long> {

    Optional<OperationType> findByType(String type);
}
