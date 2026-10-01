package com.oms.user.service;

import com.oms.user.api.dto.UserDtos.CreateUserRequest;
import com.oms.user.api.dto.UserDtos.UpdateUserRequest;
import com.oms.user.api.dto.UserDtos.UserResponse;
import com.oms.user.domain.DomainExceptions.ConflictException;
import com.oms.user.domain.DomainExceptions.NotFoundException;
import com.oms.user.domain.User;
import com.oms.user.repo.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(normalizePhone(request.phone()));
        try {
            User saved = userRepository.saveAndFlush(user);
            log.info("Created user {} ({})", saved.getId(), saved.getEmail());
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Email already exists");
        }
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        String email = normalizeEmail(request.email());
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(normalizePhone(request.phone()));
        try {
            User saved = userRepository.saveAndFlush(user);
            log.info("Updated user {}", saved.getId());
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Email already exists");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
