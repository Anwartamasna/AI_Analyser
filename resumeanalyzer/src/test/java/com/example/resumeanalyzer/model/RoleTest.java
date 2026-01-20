package com.example.resumeanalyzer.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void role_UserValue() {
        // Assert
        assertEquals("USER", Role.USER.name());
    }

    @Test
    void role_AdminValue() {
        // Assert
        assertEquals("ADMIN", Role.ADMIN.name());
    }

    @Test
    void role_ValueOf_User() {
        // Act
        Role role = Role.valueOf("USER");

        // Assert
        assertEquals(Role.USER, role);
    }

    @Test
    void role_ValueOf_Admin() {
        // Act
        Role role = Role.valueOf("ADMIN");

        // Assert
        assertEquals(Role.ADMIN, role);
    }

    @Test
    void role_InvalidValue_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> Role.valueOf("INVALID"));
    }

    @Test
    void role_Values_ContainsBoth() {
        // Act
        Role[] values = Role.values();

        // Assert
        assertEquals(2, values.length);
    }

    @Test
    void role_Ordinal() {
        // Assert
        assertEquals(0, Role.USER.ordinal());
        assertEquals(1, Role.ADMIN.ordinal());
    }
}
