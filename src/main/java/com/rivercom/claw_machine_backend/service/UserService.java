package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.User;
import com.rivercom.claw_machine_backend.dto.NewUserDTO;
import com.rivercom.claw_machine_backend.dto.UserDTO;
import com.rivercom.claw_machine_backend.mapper.UserMapper;
import com.rivercom.claw_machine_backend.repository.UserRepository;
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
    private final UserMapper mapper;

    public List<UserDTO> getAllUsers () {
        return mapper.toResponseUsers(repository.findAll());
    }

    public UserDTO getUserByPhone(String phone) {
        User user = Optional.ofNullable(
                repository.findByPhone(phone)
        ).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        return mapper.toResponseUser(user);
    }

    public UserDTO newUser (NewUserDTO newUserDTO) {
        Optional<User> existingUser = Optional.ofNullable(
                repository.findByPhone(newUserDTO.phone()));
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

    public UserDTO updateUser (UserDTO userDTO, Long id) {
        Optional<User> existingUser = repository.findById(id);
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
}
