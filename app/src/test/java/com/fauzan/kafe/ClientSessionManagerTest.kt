package com.fauzan.kafe

import com.fauzan.kafe.Model.UserModel
import com.fauzan.kafe.domain.session.ClientSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying ClientSessionManager state isolation and lifecycle.
 */
class ClientSessionManagerTest {

    private lateinit var sessionManager: ClientSessionManager

    @Before
    fun setUp() {
        sessionManager = ClientSessionManager()
    }

    @Test
    fun initialState_shouldBeEmpty() {
        assertFalse(sessionManager.isAuthenticated)
        assertNull(sessionManager.currentUser)
        assertNull(sessionManager.selectedCategory)
        assertNull(sessionManager.selectedFood)
        assertEquals("", sessionManager.currentToken)
        assertEquals("", sessionManager.authorizeKey)
    }

    @Test
    fun setCurrentUser_shouldAuthenticateSession() {
        val user = UserModel(
            name = "John Customer",
            phone = "+628987654321",
            address = "Jl. Sudirman No. 1"
        )

        sessionManager.currentUser = user

        assertTrue(sessionManager.isAuthenticated)
        assertNotNull(sessionManager.currentUser)
        assertEquals("John Customer", sessionManager.currentUser?.name)
        assertEquals("+628987654321", sessionManager.currentUser?.phone)
    }

    @Test
    fun tokenAndAuthorizationKey_shouldPersist() {
        sessionManager.currentToken = "fcm_token_sample_123"
        sessionManager.authorizeKey = "braintree_auth_key_xyz"

        assertEquals("fcm_token_sample_123", sessionManager.currentToken)
        assertEquals("braintree_auth_key_xyz", sessionManager.authorizeKey)
    }

    @Test
    fun clearSession_shouldResetAllFields() {
        val user = UserModel(name = "Alice")
        sessionManager.currentUser = user
        sessionManager.currentToken = "token"
        sessionManager.authorizeKey = "auth"

        sessionManager.clearSession()

        assertFalse(sessionManager.isAuthenticated)
        assertNull(sessionManager.currentUser)
        assertEquals("", sessionManager.currentToken)
        assertEquals("", sessionManager.authorizeKey)
    }
}
