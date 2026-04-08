package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.PointTransaction;
import com.rivercom.claw_machine_backend.domain.entity.User;
import com.rivercom.claw_machine_backend.domain.enums.TransactionType;
import com.rivercom.claw_machine_backend.dto.NewUserDTO;
import com.rivercom.claw_machine_backend.dto.UserAddPointsDTO;
import com.rivercom.claw_machine_backend.dto.UserDTO;
import com.rivercom.claw_machine_backend.dto.UserLookupDTO;
import com.rivercom.claw_machine_backend.dto.UserRemovePointsDTO;
import com.rivercom.claw_machine_backend.mapper.UserMapper;
import com.rivercom.claw_machine_backend.repository.PointTransactionRepository;
import com.rivercom.claw_machine_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserMapper mapper;

    public List<UserDTO> getAllUsers () {
        return mapper.toResponseUsers(repository.findByIsActiveTrue());
    }

    public UserDTO getUserByPhone(String phone) {
        User user = repository.findByPhoneAndIsActiveTrue(normalizePhone(phone))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El usuario no existe"));
        return mapper.toResponseUser(user);
    }

    public UserDTO newUser (NewUserDTO newUserDTO) {
        String normalizedPhone = normalizePhone(newUserDTO.phone());
        String normalizedName = normalizeNameForStorage(newUserDTO.name());

        validateUniqueActiveUser(normalizedName, normalizedPhone, null);

        Optional<User> existingPhoneUser = repository.findByPhone(normalizedPhone);
        Optional<User> existingNameUser = findUserByCanonicalName(normalizedName);

        if (existingPhoneUser.filter(user -> !Boolean.TRUE.equals(user.getIsActive())).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un usuario inactivo con ese telefono");
        }

        if (existingNameUser.filter(user -> !Boolean.TRUE.equals(user.getIsActive())).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un usuario inactivo con ese nombre");
        }

        User newUSer = new User();
        newUSer.setPhone(normalizedPhone);
        newUSer.setName(normalizedName);
        newUSer.setCurrentPoints(0);
        newUSer.setIsActive(true);
        User savedUser = repository.save(newUSer);
        return mapper.toResponseUser(savedUser);
    }

    public UserDTO updateUser (UserDTO userDTO, String phone) {
        String normalizedPhone = normalizePhone(phone);
        Optional<User> existingUser = repository.findByPhoneAndIsActiveTrue(normalizedPhone);
        if (existingUser.isEmpty()) {
            log.error("El usuario no existe");
            throw new ResponseStatusException(BAD_REQUEST, "El usuario no existe");
        }

        String updatedPhone = normalizePhone(userDTO.phone());
        String updatedName = normalizeNameForStorage(userDTO.name());
        User updatedUser = existingUser.get();
        validateUniqueActiveUser(updatedName, updatedPhone, updatedUser.getId());

        Optional<User> userWithPhone = repository.findByPhone(updatedPhone);
        if (userWithPhone.isPresent() && !userWithPhone.get().getId().equals(updatedUser.getId())) {
            throw new ResponseStatusException(CONFLICT, "Ya existe otro usuario con ese telefono");
        }

        Optional<User> userWithName = findUserByCanonicalName(updatedName);
        if (userWithName.isPresent() && !userWithName.get().getId().equals(updatedUser.getId())) {
            throw new ResponseStatusException(CONFLICT, "Ya existe otro usuario con ese nombre");
        }

        updatedUser.setPhone(updatedPhone);
        updatedUser.setName(updatedName);
        User savedUser = repository.save(updatedUser);
        return mapper.toResponseUser(savedUser);
    }

    @Transactional
    public UserDTO addPoints(UserAddPointsDTO userRequest) {
        User existingUser = repository.findByPhoneAndIsActiveTrue(normalizePhone(userRequest.phone()))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El usuario no existe"));

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

    @Transactional
    public UserDTO removePoints(UserRemovePointsDTO userRequest) {
        User existingUser = repository.findByPhoneAndIsActiveTrue(normalizePhone(userRequest.phone()))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El usuario no existe"));

        int previousBalance = existingUser.getCurrentPoints();
        int pointsToRemove = userRequest.points();

        if (pointsToRemove > previousBalance) {
            throw new ResponseStatusException(BAD_REQUEST, "El usuario no tiene puntos suficientes para realizar el ajuste");
        }

        int newBalance = previousBalance - pointsToRemove;

        existingUser.setCurrentPoints(newBalance);
        User savedUser = repository.save(existingUser);

        PointTransaction transaction = new PointTransaction();
        transaction.setUser(savedUser);
        transaction.setTransactionType(TransactionType.ADJUSTMENT);
        transaction.setPointsDelta(-pointsToRemove);
        transaction.setPreviousBalance(previousBalance);
        transaction.setNewBalance(newBalance);
        transaction.setNotes(userRequest.notes().trim());

        pointTransactionRepository.save(transaction);

        return mapper.toResponseUser(savedUser);
    }

    @Transactional
    public void deleteUser(String phone) {
        User existingUser = repository.findByPhoneAndIsActiveTrue(normalizePhone(phone))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El usuario no existe"));

        int previousBalance = existingUser.getCurrentPoints();
        existingUser.setCurrentPoints(0);
        existingUser.setIsActive(false);
        User savedUser = repository.save(existingUser);

        if (previousBalance > 0) {
            PointTransaction transaction = new PointTransaction();
            transaction.setUser(savedUser);
            transaction.setTransactionType(TransactionType.ADJUSTMENT);
            transaction.setPointsDelta(-previousBalance);
            transaction.setPreviousBalance(previousBalance);
            transaction.setNewBalance(0);
            transaction.setNotes("Usuario desactivado");
            pointTransactionRepository.save(transaction);
        }
    }

    @Transactional
    public UserDTO reactivateUser(Long id, NewUserDTO userRequest) {
        User existingUser = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El usuario no existe"));

        String normalizedName = normalizeNameForStorage(userRequest.name());
        String normalizedPhone = normalizePhone(userRequest.phone());

        validateUniqueActiveUser(normalizedName, normalizedPhone, existingUser.getId());

        Optional<User> userWithPhone = repository.findByPhone(normalizedPhone);
        if (userWithPhone.isPresent() && !userWithPhone.get().getId().equals(existingUser.getId())) {
            throw new ResponseStatusException(CONFLICT, "Ya existe otro usuario con ese telefono");
        }

        Optional<User> userWithName = findUserByCanonicalName(normalizedName);
        if (userWithName.isPresent() && !userWithName.get().getId().equals(existingUser.getId())) {
            throw new ResponseStatusException(CONFLICT, "Ya existe otro usuario con ese nombre");
        }

        existingUser.setName(normalizedName);
        existingUser.setPhone(normalizedPhone);
        existingUser.setIsActive(true);

        User savedUser = repository.save(existingUser);
        return mapper.toResponseUser(savedUser);
    }

    public UserLookupDTO lookupUser(String name, String phone) {
        String normalizedName = normalizeNameForStorage(name);
        String normalizedPhone = normalizePhone(phone);

        Optional<User> userByPhone = repository.findByPhone(normalizedPhone);
        if (userByPhone.isPresent()) {
            User user = userByPhone.get();
            return new UserLookupDTO(user.getId(), user.getName(), user.getPhone(), user.getIsActive(), "phone");
        }

        Optional<User> userByName = findUserByCanonicalName(normalizedName);
        if (userByName.isPresent()) {
            User user = userByName.get();
            return new UserLookupDTO(user.getId(), user.getName(), user.getPhone(), user.getIsActive(), "name");
        }

        throw new ResponseStatusException(BAD_REQUEST, "El usuario no existe");
    }

    private void validateUniqueActiveUser(String name, String phone, Long excludedUserId) {
        Optional<User> activeUserByPhone = repository.findByPhoneAndIsActiveTrue(phone);
        if (activeUserByPhone.isPresent() && !activeUserByPhone.get().getId().equals(excludedUserId)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un usuario activo con ese telefono");
        }

        Optional<User> activeUserByName = findActiveUserByCanonicalName(name);
        if (activeUserByName.isPresent() && !activeUserByName.get().getId().equals(excludedUserId)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un usuario activo con ese nombre");
        }
    }

    private Optional<User> findUserByCanonicalName(String name) {
        String canonicalName = canonicalizeName(name);
        return repository.findAll().stream()
                .filter(user -> canonicalizeName(user.getName()).equals(canonicalName))
                .findFirst();
    }

    private Optional<User> findActiveUserByCanonicalName(String name) {
        String canonicalName = canonicalizeName(name);
        return repository.findByIsActiveTrue().stream()
                .filter(user -> canonicalizeName(user.getName()).equals(canonicalName))
                .findFirst();
    }

    private String normalizePhone(String phone) {
        String normalizedPhone = Optional.ofNullable(phone)
                .map(String::trim)
                .orElse("");

        if (normalizedPhone.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "El telefono es obligatorio");
        }

        return normalizedPhone;
    }

    private String normalizeNameForStorage(String name) {
        String normalizedName = Optional.ofNullable(name)
                .map(this::collapseSpaces)
                .map(this::toTitleCase)
                .orElse("");

        if (normalizedName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "El nombre es obligatorio");
        }

        return normalizedName;
    }

    private String canonicalizeName(String name) {
        String collapsedName = collapseSpaces(name);
        String normalizedText = Normalizer.normalize(collapsedName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return normalizedText.toLowerCase();
    }

    private String collapseSpaces(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .map(text -> text.replaceAll("\\s+", " "))
                .orElse("");
    }

    private String toTitleCase(String value) {
        String[] parts = value.split(" ");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            String lowerPart = part.toLowerCase();
            result.append(Character.toUpperCase(lowerPart.charAt(0)));

            if (lowerPart.length() > 1) {
                result.append(lowerPart.substring(1));
            }
        }

        return result.toString();
    }
}
