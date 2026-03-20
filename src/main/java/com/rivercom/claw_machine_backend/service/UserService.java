package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.PointTransaction;
import com.rivercom.claw_machine_backend.domain.entity.User;
import com.rivercom.claw_machine_backend.domain.enums.TransactionType;
import com.rivercom.claw_machine_backend.dto.NewUserDTO;
import com.rivercom.claw_machine_backend.dto.UserAddPointsDTO;
import com.rivercom.claw_machine_backend.dto.UserDTO;
import com.rivercom.claw_machine_backend.mapper.UserMapper;
import com.rivercom.claw_machine_backend.repository.PointTransactionRepository;
import com.rivercom.claw_machine_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserMapper mapper;

    public List<UserDTO> getAllUsers () {
        return mapper.toResponseUsers(repository.findAll());
    }

    public UserDTO getUserByPhone(String phone) {
        User user = repository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        return mapper.toResponseUser(user);
    }

    public UserDTO newUser (NewUserDTO newUserDTO) {
        Optional<User> existingUser = repository.findByPhone(newUserDTO.phone());
        if (existingUser.isPresent()) {
            log.error("El usuario existe");
            return null;
        }
        User newUSer = new User();
        newUSer.setPhone(newUserDTO.phone());
        newUSer.setName(newUserDTO.name());
        newUSer.setCurrentPoints(0);
        User savedUser = repository.save(newUSer);
        return mapper.toResponseUser(savedUser);
    }

    public UserDTO updateUser (UserDTO userDTO, String phone) {
        Optional<User> existingUser = repository.findByPhone(phone);
        if (existingUser.isPresent()) {
            log.error("El usuario no existe");
            return null;
        }
        User updatedUser = new User();
        updatedUser.setPhone(userDTO.phone());
        updatedUser.setName(userDTO.name());
        User savedUser = repository.save(updatedUser);
        return mapper.toResponseUser(savedUser);
    }

    @Transactional
    public UserDTO addPoints(UserAddPointsDTO userRequest) {
        User existingUser = repository.findByPhone(userRequest.phone())
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));

        int previousBalance = existingUser.getCurrentPoints();
        int pointsToAdd = userRequest.points();
        int newBalance = previousBalance + pointsToAdd;

        existingUser.setCurrentPoints(newBalance);
        User savedUser = repository.save(existingUser);

        PointTransaction transaction = new PointTransaction();
        transaction.setUser(savedUser);
        transaction.setTransactionType(TransactionType.EARN);
        transaction.setPointsDelta(pointsToAdd);
        transaction.setPreviousBalance(previousBalance);
        transaction.setNewBalance(newBalance);

        pointTransactionRepository.save(transaction);

        return mapper.toResponseUser(savedUser);
    }
}
