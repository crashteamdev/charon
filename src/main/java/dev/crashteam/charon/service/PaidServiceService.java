package dev.crashteam.charon.service;

import dev.crashteam.charon.model.domain.PaidService;
import dev.crashteam.charon.repository.PaidServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class PaidServiceService {

    private final PaidServiceRepository paidServiceRepository;

    public PaidService getPaidServiceByTypeAndPlan(Long type, Long subscriptionType) {
        return paidServiceRepository.findByTypeAndSubscriptionType(type, subscriptionType).orElseThrow(EntityNotFoundException::new);
    }
}
