package com.rivercom.claw_machine_backend.dto;

public record UserDTO(
        Long id,
        String name,
        String phone,
        Integer points
) {
}
