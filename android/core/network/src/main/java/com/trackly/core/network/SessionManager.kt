package com.trackly.core.network

import android.content.Context
import android.util.Log
import com.trackly.core.model.User
import com.trackly.core.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("trackly_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "TracklySession"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
    }

    fun saveAuthSession(token: String, user: User) {
        Log.d(TAG, "Saving auth session for userId=${user.id}, role=${user.role}")
        prefs.edit()
            .putString(KEY_JWT_TOKEN, token)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_ROLE, user.role.name)
            .apply()
    }

    fun getJwtToken(): String? {
        return prefs.getString(KEY_JWT_TOKEN, null)
    }

    fun getUser(): User? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val roleStr = prefs.getString(KEY_USER_ROLE, UserRole.CUSTOMER.name) ?: UserRole.CUSTOMER.name
        val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.CUSTOMER }

        return User(id = id, name = name, email = email, role = role)
    }

    fun clearSession() {
        Log.d(TAG, "Clearing user auth session...")
        prefs.edit().clear().apply()
    }
}
