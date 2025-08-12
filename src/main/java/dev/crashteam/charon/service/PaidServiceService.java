package dev.crashteam.charon.service;

import dev.crashteam.charon.model.domain.PaidService;
import dev.crashteam.charon.repository.PaidServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class PaidServiceService {

    private final PaidServiceRepository paidServiceRepository;

    public PaidService getPaidServiceByTypeAndPlan(Long type, Long subscriptionType) {
        return paidServiceRepository.findByTypeAndSubscriptionType(type, subscriptionType)
                .orElseThrow(() -> new EntityNotFoundException("Not found paid service type [" + type + "] and subscription type [" + subscriptionType + "]"));
    }
}
