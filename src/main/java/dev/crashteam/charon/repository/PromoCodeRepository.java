package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.domain.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {


    Optional<PromoCode> findByCode(String code);
}
