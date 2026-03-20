package com.rivercom.claw_machine_backend.mapper;

import com.rivercom.claw_machine_backend.domain.entity.User;
import com.rivercom.claw_machine_backend.dto.UserDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserDTO toResponseUser(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getCurrentPoints()
        );
    }

    public List<UserDTO> toResponseUsers(List<User> users) {
        if (users == null) {
            return null;
        }

        return users.stream()
                .map(this::toResponseUser)
                .toList();
    }
}
