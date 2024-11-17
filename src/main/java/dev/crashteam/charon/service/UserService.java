package dev.crashteam.charon.service;

import dev.crashteam.charon.model.Currency;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public boolean userExists(String id) {
        return userRepository.existsById(id);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User getUser(String id) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("User id can't be null or empty");
        }
        if (this.userExists(id)) {
            return userRepository.findById(id).orElse(null);
        }
        log.info("Creating new user with id - {}", id);
        User user = new User();
        user.setId(id);
        user.setBalance(0L);
        user.setCurrency(Currency.RUB.getTitle());
        return this.saveUser(user);
    }
}
