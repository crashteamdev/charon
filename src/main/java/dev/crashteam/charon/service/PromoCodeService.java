package dev.crashteam.charon.service;

import dev.crashteam.charon.model.domain.PromoCode;
import dev.crashteam.charon.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    public PromoCode getPromoCode(String code) {
        return promoCodeRepository.findByCode(code).orElseThrow(EntityExistsException::new);
    }
}
