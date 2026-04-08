package com.rivercom.claw_machine_backend.controller.api;

import com.rivercom.claw_machine_backend.dto.NewUserDTO;
import com.rivercom.claw_machine_backend.dto.UserAddPointsDTO;
import com.rivercom.claw_machine_backend.dto.UserDTO;
import com.rivercom.claw_machine_backend.dto.UserLookupDTO;
import com.rivercom.claw_machine_backend.dto.UserRemovePointsDTO;
import com.rivercom.claw_machine_backend.service.UserService;
import jakarta.validation.Valid;
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

    @GetMapping("/lookup")
    public ResponseEntity<UserLookupDTO> lookupUser(
            @RequestParam String name,
            @RequestParam String phone
    ) {
        return ResponseEntity.ok(service.lookupUser(name, phone));
    }

    @PutMapping("/{phone}")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO user, @PathVariable String phone) {
        return ResponseEntity.ok(service.updateUser(user, phone));
    }

    @PutMapping("/add-points")
    public ResponseEntity<UserDTO> addPoints(@RequestBody UserAddPointsDTO user) {
        return ResponseEntity.ok(service.addPoints(user));
    }

    @PutMapping("/remove-points")
    public ResponseEntity<UserDTO> removePoints(@Valid @RequestBody UserRemovePointsDTO user) {
        return ResponseEntity.ok(service.removePoints(user));
    }

    @DeleteMapping("/{phone}")
    public ResponseEntity<Void> deleteUser(@PathVariable String phone) {
        service.deleteUser(phone);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<UserDTO> reactivateUser(
            @PathVariable Long id,
            @RequestBody NewUserDTO user
    ) {
        return ResponseEntity.ok(service.reactivateUser(id, user));
    }
}
