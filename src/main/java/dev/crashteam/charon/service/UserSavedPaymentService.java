package dev.crashteam.charon.service;

import dev.crashteam.charon.model.domain.UserSavedPayment;
import dev.crashteam.charon.repository.UserSavedPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSavedPaymentService {

    private final UserSavedPaymentRepository paymentRepository;

    public List<UserSavedPayment> findAll() {
        return paymentRepository.findAll();
    }

    public UserSavedPayment findByUserId(String userId) {
        return paymentRepository.findByUserId(userId).orElse(null);
    }

    public void cancelRecurrentPayment(String userId) {
        UserSavedPayment userSavedPayment = paymentRepository.findByUserId(userId).orElseThrow(EntityNotFoundException::new);
        log.info("Deleting saved payment of user - {}", userId);
        paymentRepository.delete(userSavedPayment);
    }

    @Transactional
    public void save(UserSavedPayment savedPayment) {
        paymentRepository.save(savedPayment);
    }
}
