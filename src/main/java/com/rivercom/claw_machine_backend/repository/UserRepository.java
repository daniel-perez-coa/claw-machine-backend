package com.rivercom.claw_machine_backend.repository;

import com.rivercom.claw_machine_backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
}
