package com.trackly.core.network

import android.util.Log
import com.trackly.core.common.network.Resource
import com.trackly.core.model.User
import com.trackly.core.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun register(name: String, email: String, password: String, role: UserRole, vehicleNumber: String?): Resource<User>
    suspend fun login(email: String, password: String): Resource<User>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    companion object {
        private const val TAG = "TracklyAuthRepo"
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

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val user = User(
                    id = body.user.id,
                    name = body.user.name,
                    email = body.user.email,
                    role = try { UserRole.valueOf(body.user.role) } catch (e: Exception) { UserRole.CUSTOMER }
                )
                sessionManager.saveAuthSession(body.token, user)
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

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val user = User(
                    id = body.user.id,
                    name = body.user.name,
                    email = body.user.email,
                    role = try { UserRole.valueOf(body.user.role) } catch (e: Exception) { UserRole.CUSTOMER }
                )
                sessionManager.saveAuthSession(body.token, user)
                Log.d(TAG, "Login successful. JWT token & User session stored.")
                Resource.Success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Invalid email or password"
                Log.w(TAG, "Login error: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during login", e)
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }
}
