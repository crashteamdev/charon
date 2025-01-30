package dev.crashteam.charon.repository;

import dev.crashteam.charon.model.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    @Query(value = "SELECT u.* FROM app_user u " +
            "WHERE u.subscription_valid_until\\:\\:date = now()\\:\\:date", nativeQuery = true)
    List<User> findTodaySubEnds();
}
