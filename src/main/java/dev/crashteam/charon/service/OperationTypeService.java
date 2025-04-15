package dev.crashteam.charon.service;

import dev.crashteam.charon.model.domain.OperationType;
import dev.crashteam.charon.repository.OperationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class OperationTypeService {

    private final OperationTypeRepository operationTypeRepository;

    public OperationType getOperationType(String type) {
        return operationTypeRepository.findByType(type).orElseThrow(EntityNotFoundException::new);
    }
}
