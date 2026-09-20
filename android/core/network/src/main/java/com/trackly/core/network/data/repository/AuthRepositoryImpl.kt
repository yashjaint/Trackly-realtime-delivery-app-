package com.trackly.core.network.data.repository

import android.util.Log
import com.trackly.core.common.network.Resource
import com.trackly.core.model.User
import com.trackly.core.model.UserRole
import com.trackly.core.network.ApiLoginRequest
import com.trackly.core.network.ApiRegisterRequest
import com.trackly.core.network.ApiUpdateProfileRequest
import com.trackly.core.network.AuthApi
import com.trackly.core.network.SessionManager
import com.trackly.core.network.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    companion object {
        private const val TAG = "TracklyAuthRepo"
    }

    private fun getAuthHeader(): String {
        val token = sessionManager.getJwtToken() ?: ""
        return "Bearer $token"
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        vehicleNumber: String?
    ): Resource<User> {
        Log.d(TAG, "Initiating registration API call for email=$email, role=$role")
        return try {
            val response = authApi.register(
                ApiRegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                    role = role.name,
                    vehicleNumber = vehicleNumber
                )
            )

            val responseBody = response.body()
            if (response.isSuccessful && responseBody != null) {
                val user = User(
                    id = responseBody.user.id,
                    name = responseBody.user.name,
                    email = responseBody.user.email,
                    role = try { UserRole.valueOf(responseBody.user.role) } catch (e: Exception) { UserRole.CUSTOMER }
                )
                sessionManager.saveAuthSession(responseBody.token, user)
                Log.d(TAG, "Registration successful. User saved in SessionManager: ${user.id}")
                Resource.Success(user)
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                val parsedMsg = try {
                    org.json.JSONObject(rawError).optString("message", rawError)
                } catch (e: Exception) {
                    if (rawError.isNotBlank()) rawError else "Registration failed (${response.code()})"
                }
                val isDuplicate = response.code() == 409 ||
                        parsedMsg.contains("already registered", ignoreCase = true) ||
                        parsedMsg.contains("already exists", ignoreCase = true)
                val cleanErrorMsg = if (isDuplicate) "User is already registered with this email" else parsedMsg
                Log.w(TAG, "Registration error: $cleanErrorMsg (raw: $rawError)")
                Resource.Error(cleanErrorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during registration", e)
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    override suspend fun login(email: String, password: String): Resource<User> {
        Log.d(TAG, "Initiating login API call for email=$email")
        return try {
            val response = authApi.login(ApiLoginRequest(email = email, password = password))

            val responseBody = response.body()
            if (response.isSuccessful && responseBody != null) {
                val user = User(
                    id = responseBody.user.id,
                    name = responseBody.user.name,
                    email = responseBody.user.email,
                    role = try { UserRole.valueOf(responseBody.user.role) } catch (e: Exception) { UserRole.CUSTOMER }
                )
                sessionManager.saveAuthSession(responseBody.token, user)
                Log.d(TAG, "Login successful. JWT token & User session stored.")
                Resource.Success(user)
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                val parsedMsg = try {
                    org.json.JSONObject(rawError).optString("message", rawError)
                } catch (e: Exception) {
                    if (rawError.isNotBlank()) rawError else "Invalid email or password"
                }
                Log.w(TAG, "Login error: $parsedMsg (raw: $rawError)")
                Resource.Error(parsedMsg)
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Socket timeout during login", e)
            Resource.Error("Request timed out. Please verify server connection.")
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "Connect exception during login", e)
            Resource.Error("Could not connect to server.")
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during login", e)
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    override suspend fun updateProfile(name: String, vehicleNumber: String?): Resource<User> {
        Log.d(TAG, "Initiating update profile API call for name=$name")
        return try {
            val response = authApi.updateProfile(
                authHeader = getAuthHeader(),
                request = ApiUpdateProfileRequest(name = name, vehicleNumber = vehicleNumber)
            )
            val responseBody = response.body()
            if (response.isSuccessful && responseBody != null) {
                val currentUser = sessionManager.getUser()
                val updatedUser = User(
                    id = responseBody.id,
                    name = responseBody.name,
                    email = responseBody.email,
                    role = currentUser?.role ?: try { UserRole.valueOf(responseBody.role) } catch (e: Exception) { UserRole.CUSTOMER }
                )
                val token = sessionManager.getJwtToken() ?: ""
                sessionManager.saveAuthSession(token, updatedUser)
                Log.d(TAG, "Profile updated successfully: ${updatedUser.name}")
                Resource.Success(updatedUser)
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                val parsedMsg = try {
                    org.json.JSONObject(rawError).optString("message", rawError)
                } catch (e: Exception) {
                    "Failed to update profile (${response.code()})"
                }
                Resource.Error(parsedMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during profile update", e)
            Resource.Error(e.localizedMessage ?: "Network error during profile update")
        }
    }

    override suspend fun deleteAccount(): Resource<Unit> {
        Log.d(TAG, "Initiating delete account API call...")
        return try {
            val response = authApi.deleteAccount(authHeader = getAuthHeader())
            if (response.isSuccessful) {
                sessionManager.clearSession()
                Log.d(TAG, "Account deleted and session cleared.")
                Resource.Success(Unit)
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                val parsedMsg = try {
                    org.json.JSONObject(rawError).optString("message", rawError)
                } catch (e: Exception) {
                    if (rawError.isNotBlank()) rawError else "Failed to delete account (${response.code()})"
                }
                Log.w(TAG, "Delete account failed: $parsedMsg")
                Resource.Error(parsedMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during account deletion", e)
            Resource.Error(e.localizedMessage ?: "Network error during account deletion")
        }
    }
}
