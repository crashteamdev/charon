package dev.crashteam.charon.service;

import dev.crashteam.charon.model.domain.PromoCode;
import dev.crashteam.charon.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityExistsException;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    public PromoCode getPromoCode(String code) {
        return promoCodeRepository.findByCode(code).orElse(null);
    }

    public boolean existsByCodeAndUserId(String code, String userId ) {
        return promoCodeRepository.findByCodeAndUserId(code, userId).isPresent();
    }

    public PromoCode save(PromoCode promoCode) {
        return promoCodeRepository.save(promoCode);
    }
}
