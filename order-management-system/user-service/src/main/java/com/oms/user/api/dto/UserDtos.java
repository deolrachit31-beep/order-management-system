package com.oms.user.api.dto;

import com.oms.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 30) String phone) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 30) String phone) {
    }

    public record UserResponse(
            UUID id,
            String name,
            String email,
            String phone,
            Instant createdAt,
            Instant updatedAt) {

        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getCreatedAt(),
                    user.getUpdatedAt());
        }
    }
}
