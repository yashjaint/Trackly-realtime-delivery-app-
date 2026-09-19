package com.trackly.features.auth.routes

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
            val request = call.receive<RegisterRequest>()
            try {
                val response = authService.register(request)
                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                val isAlreadyExists = e.message?.contains("already exists", ignoreCase = true) == true
                val statusCode = if (isAlreadyExists) HttpStatusCode.Conflict else HttpStatusCode.BadRequest
                val msg = if (isAlreadyExists) "User is already registered with this email" else (e.message ?: "Invalid request")
                call.respond(
                    statusCode,
                    mapOf("status" to statusCode.value, "message" to msg)
                )
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            try {
                val response = authService.login(request)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("status" to 401, "message" to (e.message ?: "Invalid credentials"))
                )
            }
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Invalid token principal"))
                    return@get
                }
                try {
                    val userProfile = authService.getUserProfile(userId)
                    call.respond(HttpStatusCode.OK, userProfile)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found"))
                }
            }
        }
    }
}
