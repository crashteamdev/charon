package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.domain.UserSavedPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSavedPaymentRepository extends JpaRepository<UserSavedPayment, Long> {

    Optional<UserSavedPayment> findByUserId(String userId);
}
