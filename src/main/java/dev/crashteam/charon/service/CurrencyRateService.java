package dev.crashteam.charon.service;

import dev.crashteam.charon.model.domain.CurrencyRate;
import dev.crashteam.charon.repository.CurrencyRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class CurrencyRateService {

    private final CurrencyRateRepository rateRepository;

    public CurrencyRate getByCurrencyAndInitCurrency(String currency, String initCurrency) {
        return rateRepository.findByCurrencyAndInitCurrency(currency, initCurrency)
                .orElse(null);
    }

    public CurrencyRate save(CurrencyRate currencyRate) {
        return rateRepository.save(currencyRate);
    }
}
