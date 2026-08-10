package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Authority;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeletionCleanupService userDeletionCleanupService;

    @Mock
    private FindAuthenticatedUser findAuthenticatedUser;

    @Mock
    private AdminAuditService adminAuditService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void deleteUser_shouldLogBeforeDeletingTargetUser() {
        User actor = new User();
        actor.setId(10L);
        actor.setFirstName("Admin");
        actor.setLastName("User");
        actor.setAuthorities(List.of(new Authority("ROLE_ADMIN")));

        User target = new User();
        target.setId(20L);
        target.setFirstName("Jane");
        target.setLastName("Doe");

        when(findAuthenticatedUser.getAuthenticatedUser()).thenReturn(actor);
        when(userRepository.findById(20L)).thenReturn(Optional.of(target));

        adminUserService.deleteUser(20L);

        InOrder inOrder = inOrder(userDeletionCleanupService, adminAuditService, userRepository);
        inOrder.verify(userDeletionCleanupService).detachUserBeforeDelete(20L);
        inOrder.verify(adminAuditService).log(actor, null, "USER_DELETE", "Admin User deleted user Jane Doe");
        inOrder.verify(userRepository).delete(target);
    }

    @Test
    void deleteUser_shouldRejectDeletingOwnAccount() {
        User actor = new User();
        actor.setId(10L);
        actor.setFirstName("Admin");
        actor.setLastName("User");

        User target = new User();
        target.setId(10L);
        target.setFirstName("Admin");
        target.setLastName("User");

        when(findAuthenticatedUser.getAuthenticatedUser()).thenReturn(actor);
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> adminUserService.deleteUser(10L));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        verify(userDeletionCleanupService, never()).detachUserBeforeDelete(anyLong());
        verify(adminAuditService, never()).log(any(), any(), any(), any());
        verify(userRepository, never()).delete(any());
    }
}
