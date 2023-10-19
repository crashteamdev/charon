package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.domain.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String>, JpaSpecificationExecutor<Payment> {

    @Query("select p from Payment p where p.userId = :userId")
    List<Payment> findByUserId(String userId, Pageable pageable);

    Optional<Payment> findByOperationId(String operationId);

    @Query("select p from Payment p where p.paymentId = :userId")
    Optional<Payment> findByPaymentId(String userId);

    List<Payment> findAllByStatus(String status);

    @Query(value = "SELECT pr.* FROM payment_repository pr " +
            "INNER JOIN operation_type ot ON pr.operation_type_id = ot.id " +
            "WHERE pr.status = ?1 AND ot.type = ?2", nativeQuery = true)
    List<Payment> findAllByStatusAndOperationType(String status, String operationType);
}
