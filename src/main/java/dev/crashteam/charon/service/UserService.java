package dev.crashteam.charon.service;

import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public boolean userExists(String id) {
        return userRepository.existsById(id);
    }

    public User getUser(String id) {
        return userRepository.findById(id).orElse(null);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
