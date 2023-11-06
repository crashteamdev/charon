package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String>, JpaSpecificationExecutor<Payment> {

    @Query(value = "SELECT nextval('operation_id_seq')", nativeQuery = true)
    Long operationIdSeq();

    @Query("select p from Payment p where p.userId = :userId")
    List<Payment> findByUserId(String userId, Pageable pageable);

    Optional<Payment> findByOperationId(String operationId);

    @Query("select p from Payment p where p.paymentId = :paymentId")
    Optional<Payment> findByPaymentId(String paymentId);

    @Query("SELECT p FROM Payment p WHERE p.created >= ?1 AND p.created <= ?2")
    List<Payment> findByCreatedAtBetween(Date from, Date to);

    @Query(value = "SELECT pr.* FROM payment pr " +
            "INNER JOIN operation_type ot ON pr.operation_type_id = ot.id " +
            "WHERE pr.status = 'PENDING' AND ot.type = ?1", nativeQuery = true)
    List<Payment> findAllByPendingStatusAndOperationType(String operationType);

    @Query(value = "SELECT pr.* FROM payment pr " +
            "INNER JOIN operation_type ot ON pr.operation_type_id = ot.id " +
            "WHERE (pr.status = ?1 AND ot.type = ?2) " +
            "AND (p.created >= ?3 AND p.created <= ?4)", nativeQuery = true)
    List<Payment> findAllByStatusAndOperationTypeAndCreatedBetween(RequestPaymentStatus status, String operationType,
                                                                   LocalDateTime from, LocalDateTime to);
}
