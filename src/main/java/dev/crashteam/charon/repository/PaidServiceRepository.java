package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.domain.PaidService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaidServiceRepository extends JpaRepository<PaidService, Long> {

    @Query("SELECT ps FROM PaidService ps INNER JOIN SubscriptionType s " +
            "ON s.id = ps.subscriptionTypeId " +
            "WHERE ps.type = ?1 and s.type = ?2")
    Optional<PaidService> findByTypeAndSubscriptionType(Long type, Long subscriptionType);

}
