package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);

    Optional<User> findByPhoneAndIsActiveTrue(String phone);

    Optional<User> findByNameIgnoreCase(String name);

    Optional<User> findByNameIgnoreCaseAndIsActiveTrue(String name);

    List<User> findByIsActiveTrue();

    List<User> findByCurrentPointsGreaterThan(Integer currentPoints);
}
