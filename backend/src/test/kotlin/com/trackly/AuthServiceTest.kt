package com.trackly

import com.trackly.core.database.DatabaseFactory
import com.trackly.features.auth.dto.LoginRequest
import com.trackly.features.auth.dto.RegisterRequest
import com.trackly.features.auth.service.AuthService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AuthServiceTest {

    private lateinit var authService: AuthService

    @BeforeTest
    fun setup() {
        DatabaseFactory.init()
        authService = AuthService()
    }

    @Test
    fun `register customer user successfully returns token and user dto`() {
        val email = "testcustomer_${System.currentTimeMillis()}@trackly.com"
        val registerReq = RegisterRequest(
            name = "Test Customer",
            email = email,
            password = "SecurePassword123!",
            role = "CUSTOMER"
        )

        val response = authService.register(registerReq)

        assertNotNull(response.token)
        assertEquals(email, response.user.email)
        assertEquals("CUSTOMER", response.user.role)
    }

    @Test
    fun `register driver user successfully creates driver record`() {
        val email = "testdriver_${System.currentTimeMillis()}@trackly.com"
        val registerReq = RegisterRequest(
            name = "Test Driver",
            email = email,
            password = "SecurePassword123!",
            role = "DRIVER",
            vehicleNumber = "KA-01-AB-1234"
        )

        val response = authService.register(registerReq)

        assertNotNull(response.token)
        assertEquals(email, response.user.email)
        assertEquals("DRIVER", response.user.role)
    }

    @Test
    fun `register duplicate email fails with exception`() {
        val email = "duplicate_${System.currentTimeMillis()}@trackly.com"
        val registerReq = RegisterRequest(
            name = "First User",
            email = email,
            password = "Password123!",
            role = "CUSTOMER"
        )

        authService.register(registerReq)

        assertFailsWith<IllegalArgumentException> {
            authService.register(registerReq)
        }
    }

    @Test
    fun `login with correct credentials succeeds`() {
        val email = "login_test_${System.currentTimeMillis()}@trackly.com"
        val password = "Password123!"
        authService.register(RegisterRequest("Login User", email, password, "CUSTOMER"))

        val loginResponse = authService.login(LoginRequest(email, password))

        assertNotNull(loginResponse.token)
        assertEquals(email, loginResponse.user.email)
    }

    @Test
    fun `login with wrong password fails`() {
        val email = "wrongpass_${System.currentTimeMillis()}@trackly.com"
        authService.register(RegisterRequest("User", email, "CorrectPass", "CUSTOMER"))

        assertFailsWith<IllegalArgumentException> {
            authService.login(LoginRequest(email, "WrongPass"))
        }
    }
}
