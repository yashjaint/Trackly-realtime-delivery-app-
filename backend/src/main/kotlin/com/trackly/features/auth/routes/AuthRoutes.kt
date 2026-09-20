package com.trackly.features.auth.routes

import com.trackly.features.auth.dto.ErrorResponse
import com.trackly.features.auth.dto.LoginRequest
import com.trackly.features.auth.dto.RegisterRequest
import com.trackly.features.auth.service.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/api/v1/auth") {
        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
                val response = authService.register(request)
                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                val isAlreadyExists = e.message?.contains("already exists", ignoreCase = true) == true
                val statusCode = if (isAlreadyExists) HttpStatusCode.Conflict else HttpStatusCode.BadRequest
                val msg = if (isAlreadyExists) "User is already registered with this email" else (e.message ?: "Invalid request")
                call.respond(
                    statusCode,
                    ErrorResponse(message = msg, status = statusCode.value)
                )
            } catch (e: Throwable) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(message = e.message ?: "Registration failed", status = 400)
                )
            }
        }

        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                val response = authService.login(request)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(message = e.message ?: "Invalid email or password", status = 401)
                )
            } catch (e: Throwable) {
                call.application.environment.log.error("Unhandled exception during login", e)
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(message = e.message ?: "Login failed. Please check your credentials.", status = 401)
                )
            }
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(message = "Invalid token principal", status = 401))
                    return@get
                }
                try {
                    val userProfile = authService.getUserProfile(userId)
                    call.respond(HttpStatusCode.OK, userProfile)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(message = "User not found", status = 404))
                }
            }
        }
    }
}
