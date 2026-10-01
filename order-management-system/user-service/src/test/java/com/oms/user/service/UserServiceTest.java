package com.oms.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oms.user.api.dto.UserDtos.CreateUserRequest;
import com.oms.user.api.dto.UserDtos.UserResponse;
import com.oms.user.domain.DomainExceptions.ConflictException;
import com.oms.user.domain.DomainExceptions.NotFoundException;
import com.oms.user.domain.User;
import com.oms.user.repo.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void createNormalizesEmailAndPhone() {
        when(userRepository.existsByEmail("ava.shah@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        UserResponse response = userService.create(
                new CreateUserRequest("  Ava Shah  ", "  Ava.Shah@Example.com ", "  +1-555-0100  "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("ava.shah@example.com");
        assertThat(captor.getValue().getName()).isEqualTo("Ava Shah");
        assertThat(captor.getValue().getPhone()).isEqualTo("+1-555-0100");
        assertThat(response.email()).isEqualTo("ava.shah@example.com");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ava@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new CreateUserRequest("Ava Shah", "ava@example.com", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already exists");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void getMissingUserThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.get(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void blankPhoneIsStoredAsNull() {
        assertThat(UserService.normalizePhone("   ")).isNull();
        assertThat(UserService.normalizePhone(null)).isNull();
    }
}
