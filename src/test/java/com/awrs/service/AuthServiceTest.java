package com.awrs.service;

import com.awrs.model.User;
import com.awrs.model.User.Role;
import com.awrs.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Demo 1 — Test Suite: AuthService
 *
 * Covers: login success/failure, session management, RBAC permission checks,
 *         user creation (admin-only), user deactivation.
 *
 * Maps to SRS §2.1 — User Authentication and Role-Based Access Control
 * Uses Mockito to stub UserRepository (no real DB needed for Demo 1).
 */
@DisplayName("Demo 1 | AuthService Tests")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository mockUserRepo;

    private AuthService authService;

    private static final String RAW_PASSWORD = "secret123";
    private static final String HASHED       = AuthService.hash("secret123");

    private User adminUser;
    private User workerUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(mockUserRepo);
        adminUser   = new User(1, "admin",  HASHED, Role.ADMIN);
        workerUser  = new User(2, "worker", HASHED, Role.WORKER);
    }

    // --- Login ---

    @Test
    @DisplayName("Login succeeds with correct credentials")
    void testLogin_Success() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        assertTrue(authService.login("admin", RAW_PASSWORD));
    }

    @Test
    @DisplayName("Login sets currentUser on success")
    void testLogin_SetsCurrentUser() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        authService.login("admin", RAW_PASSWORD);
        assertNotNull(authService.getCurrentUser());
        assertEquals("admin", authService.getCurrentUser().getUsername());
    }

    @Test
    @DisplayName("Login fails with wrong password")
    void testLogin_WrongPassword() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        assertFalse(authService.login("admin", "wrongpassword"));
    }

    @Test
    @DisplayName("Login fails when username does not exist")
    void testLogin_UserNotFound() {
        when(mockUserRepo.findByUsername("ghost")).thenReturn(Optional.empty());
        assertFalse(authService.login("ghost", RAW_PASSWORD));
    }

    @Test
    @DisplayName("Login fails for deactivated account")
    void testLogin_DeactivatedUser() {
        adminUser.setActive(false);
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        assertFalse(authService.login("admin", RAW_PASSWORD));
    }

    @Test
    @DisplayName("Login fails with blank username")
    void testLogin_BlankUsername() {
        assertFalse(authService.login("", RAW_PASSWORD));
        verifyNoInteractions(mockUserRepo);
    }

    @Test
    @DisplayName("Login fails with null password")
    void testLogin_NullPassword() {
        assertFalse(authService.login("admin", null));
    }

    // --- Logout ---

    @Test
    @DisplayName("Logout clears currentUser")
    void testLogout_ClearsSession() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        authService.login("admin", RAW_PASSWORD);
        authService.logout();
        assertNull(authService.getCurrentUser());
    }

    // --- RBAC Permission Checks ---

    @Test
    @DisplayName("ADMIN has permission for ADMIN-level actions")
    void testHasPermission_AdminCanDoAdmin() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        authService.login("admin", RAW_PASSWORD);
        assertTrue(authService.hasPermission(Role.ADMIN));
    }

    @Test
    @DisplayName("ADMIN has permission for WORKER-level actions")
    void testHasPermission_AdminCanDoWorker() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        authService.login("admin", RAW_PASSWORD);
        assertTrue(authService.hasPermission(Role.WORKER));
    }

    @Test
    @DisplayName("WORKER does not have ADMIN permission")
    void testHasPermission_WorkerCannotDoAdmin() {
        when(mockUserRepo.findByUsername("worker")).thenReturn(Optional.of(workerUser));
        authService.login("worker", RAW_PASSWORD);
        assertFalse(authService.hasPermission(Role.ADMIN));
    }

    @Test
    @DisplayName("hasPermission returns false when not logged in")
    void testHasPermission_NotLoggedIn() {
        assertFalse(authService.hasPermission(Role.WORKER));
    }

    // --- User Creation (Admin only) ---

    @Test
    @DisplayName("Admin can create a new user")
    void testCreateUser_AdminSuccess() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(mockUserRepo.findByUsername("newworker")).thenReturn(Optional.empty());
        authService.login("admin", RAW_PASSWORD);
        assertTrue(authService.createUser("newworker", "pass456", Role.WORKER));
        verify(mockUserRepo, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Worker cannot create a new user")
    void testCreateUser_WorkerForbidden() {
        when(mockUserRepo.findByUsername("worker")).thenReturn(Optional.of(workerUser));
        authService.login("worker", RAW_PASSWORD);
        assertFalse(authService.createUser("anotherUser", "pass", Role.WORKER));
        verify(mockUserRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Cannot create user with duplicate username")
    void testCreateUser_DuplicateUsername() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        authService.login("admin", RAW_PASSWORD);
        when(mockUserRepo.findByUsername("existingUser")).thenReturn(Optional.of(workerUser));
        assertFalse(authService.createUser("existingUser", "pass", Role.WORKER));
    }

    // --- Deactivation ---

    @Test
    @DisplayName("Admin can deactivate a user")
    void testDeactivateUser_AdminSuccess() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        authService.login("admin", RAW_PASSWORD);
        when(mockUserRepo.findById(2)).thenReturn(Optional.of(workerUser));
        assertTrue(authService.deactivateUser(2));
        assertFalse(workerUser.isActive());
        verify(mockUserRepo).update(workerUser);
    }

    @Test
    @DisplayName("Deactivation fails for non-existent user id")
    void testDeactivateUser_NotFound() {
        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        authService.login("admin", RAW_PASSWORD);
        when(mockUserRepo.findById(999)).thenReturn(Optional.empty());
        assertFalse(authService.deactivateUser(999));
    }

    // --- Hash utility ---

    @Test
    @DisplayName("hash() produces consistent SHA-256 output")
    void testHash_Deterministic() {
        assertEquals(AuthService.hash("abc"), AuthService.hash("abc"));
    }

    @Test
    @DisplayName("hash() produces different output for different inputs")
    void testHash_UniquenessForDifferentInputs() {
        assertNotEquals(AuthService.hash("abc"), AuthService.hash("xyz"));
    }

    @Test
    @DisplayName("hash() produces 64-character hex string")
    void testHash_Length() {
        assertEquals(64, AuthService.hash("test").length());
    }
}
