package com.rivercom.claw_machine_backend.dto;

public record PrizeLookupDTO(
        Long id,
        String name,
        Boolean isActive
) {
}
