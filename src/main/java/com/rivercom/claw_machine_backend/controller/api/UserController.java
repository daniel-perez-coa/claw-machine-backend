package com.rivercom.claw_machine_backend.controller.api;

import com.rivercom.claw_machine_backend.dto.NewUserDTO;
import com.rivercom.claw_machine_backend.dto.UserAddPointsDTO;
import com.rivercom.claw_machine_backend.dto.UserDTO;
import com.rivercom.claw_machine_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/users")
public class UserController {

    private final UserService service;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @GetMapping("/by-phone/{phone}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String phone) {
        return ResponseEntity.ok(service.getUserByPhone(phone));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody NewUserDTO user) {
        return ResponseEntity.ok(service.newUser(user));
    }

    @PutMapping("/{phone}")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO user, @PathVariable String phone) {
        return ResponseEntity.ok(service.updateUser(user, phone));
    }

    @PutMapping("/add-points")
    public ResponseEntity<UserDTO> addPoints(@RequestBody UserAddPointsDTO user) {
        return ResponseEntity.ok(service.addPoints(user));
    }
}
