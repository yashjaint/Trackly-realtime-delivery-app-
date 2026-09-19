package com.trackly.features.driver.routes

import com.trackly.features.driver.dto.UpdateLocationRequest
import com.trackly.features.driver.service.DriverService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.driverRoutes(driverService: DriverService) {
    authenticate("auth-jwt") {
        route("/api/v1/driver") {
            post("/location") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Invalid token"))

                val request = call.receive<UpdateLocationRequest>()
                val success = driverService.updateLocation(userId, request.lat, request.lng)

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("status" to "success", "message" to "Location updated"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "Driver profile not found"))
                }
            }
        }
    }
}
