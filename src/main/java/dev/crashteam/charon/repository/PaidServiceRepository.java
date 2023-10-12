package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.domain.PaidService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaidServiceRepository extends JpaRepository<PaidService, Long> {

    Optional<PaidService> findByTypeAndPlan(String type, String plan);

}
