package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.domain.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {


    Optional<PromoCode> findByCode(String code);


    @Query(value = "SELECT pc.* FROM promo_code pc " +
            " INNER JOIN promo_code_app_user pu " +
            " ON pu.promo_code_id = pc.id " +
            " INNER JOIN app_user au ON au.id = pu.user_id " +
            " WHERE pc.code = ?1 AND au.id = ?2 ", nativeQuery = true)
    Optional<PromoCode> findByCodeAndUserId(String code, String userId);
}
