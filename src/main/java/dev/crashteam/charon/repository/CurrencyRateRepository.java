package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.domain.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {

    Optional<CurrencyRate> findByCurrencyAndInitCurrency(String currency, String initCurrency);

}
