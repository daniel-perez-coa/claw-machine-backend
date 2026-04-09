package com.rivercom.claw_machine_backend.dto;

public record UserAddPointsResponseDTO(
        UserDTO user,
        Long transactionId
) {
}
