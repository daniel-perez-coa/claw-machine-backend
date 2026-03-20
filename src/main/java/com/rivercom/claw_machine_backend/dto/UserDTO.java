package com.rivercom.claw_machine_backend.dto;

public record UserDTO(
        String name,
        String phone,
        Integer points
) {
}
