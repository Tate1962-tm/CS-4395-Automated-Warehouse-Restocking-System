package com.awrs.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Demo 1 — Test Suite: User Model
 *
 * Covers: User construction, role assignment, active flag,
 *         and SHA-256 password hash utility.
 *
 * Maps to SRS §2.1 — User Authentication and Role-Based Access Control
 */
@DisplayName("Demo 1 | User Model Tests")
class UserTest {

    private User adminUser;
    private User managerUser;
    private User workerUser;

    @BeforeEach
    void setUp() {
        adminUser   = new User(1, "admin",   "hashedPass1", User.Role.ADMIN);
        managerUser = new User(2, "manager", "hashedPass2", User.Role.MANAGER);
        workerUser  = new User(3, "worker",  "hashedPass3", User.Role.WORKER);
    }

    // --- Construction ---

    @Test
    @DisplayName("User is created with correct id and username")
    void testUserCreation_IdAndUsername() {
        assertEquals(1, adminUser.getId());
        assertEquals("admin", adminUser.getUsername());
    }

    @Test
    @DisplayName("User role is assigned correctly at construction")
    void testUserCreation_RoleAssignment() {
        assertEquals(User.Role.ADMIN,   adminUser.getRole());
        assertEquals(User.Role.MANAGER, managerUser.getRole());
        assertEquals(User.Role.WORKER,  workerUser.getRole());
    }

    @Test
    @DisplayName("New user is active by default")
    void testUserCreation_ActiveByDefault() {
        assertTrue(adminUser.isActive());
        assertTrue(workerUser.isActive());
    }

    // --- Setters ---

    @Test
    @DisplayName("setActive(false) deactivates the user")
    void testSetActive_Deactivation() {
        adminUser.setActive(false);
        assertFalse(adminUser.isActive());
    }

    @Test
    @DisplayName("setRole changes the user role")
    void testSetRole() {
        workerUser.setRole(User.Role.MANAGER);
        assertEquals(User.Role.MANAGER, workerUser.getRole());
    }

    @Test
    @DisplayName("setUsername updates the username field")
    void testSetUsername() {
        adminUser.setUsername("superadmin");
        assertEquals("superadmin", adminUser.getUsername());
    }

    @Test
    @DisplayName("setPasswordHash updates stored hash")
    void testSetPasswordHash() {
        adminUser.setPasswordHash("newHash");
        assertEquals("newHash", adminUser.getPasswordHash());
    }

    // --- Role Ordinal (RBAC hierarchy: ADMIN=0, MANAGER=1, WORKER=2) ---

    @Test
    @DisplayName("ADMIN role ordinal is lower than MANAGER")
    void testRoleHierarchy_AdminVsManager() {
        assertTrue(User.Role.ADMIN.ordinal() < User.Role.MANAGER.ordinal());
    }

    @Test
    @DisplayName("MANAGER role ordinal is lower than WORKER")
    void testRoleHierarchy_ManagerVsWorker() {
        assertTrue(User.Role.MANAGER.ordinal() < User.Role.WORKER.ordinal());
    }

    // --- toString ---

    @Test
    @DisplayName("toString includes username and role")
    void testToString_ContainsKeyFields() {
        String str = adminUser.toString();
        assertTrue(str.contains("admin"));
        assertTrue(str.contains("ADMIN"));
    }

    // --- No-arg constructor ---

    @Test
    @DisplayName("No-arg constructor produces non-null User with default values")
    void testNoArgConstructor() {
        User blank = new User();
        assertNotNull(blank);
        assertFalse(blank.isActive()); // boolean default is false
        assertNull(blank.getUsername());
    }
}
