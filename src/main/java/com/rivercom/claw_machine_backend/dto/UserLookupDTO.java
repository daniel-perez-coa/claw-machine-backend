package com.rivercom.claw_machine_backend.dto;

public record UserLookupDTO(
        Long id,
        String name,
        String phone,
        Boolean isActive,
        String matchedBy
) {
}
