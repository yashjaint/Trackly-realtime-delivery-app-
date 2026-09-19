package com.trackly

import com.trackly.core.database.DatabaseFactory
import com.trackly.core.security.JwtConfig
import com.trackly.features.auth.routes.authRoutes
import com.trackly.features.auth.service.AuthService
import com.trackly.features.order.routes.orderRoutes
import com.trackly.features.order.service.OrderService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration

import com.trackly.features.driver.routes.driverRoutes
import com.trackly.features.driver.service.DriverService
import com.trackly.features.tracking.routes.trackingRoutes

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("TracklyBackend")
    logger.info("Starting Trackly Ktor Backend Server...")

    DatabaseFactory.init()

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.AUDIENCE
            verifier(JwtConfig.makeVerifier())
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { defaultScheme, realm ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("status" to 401, "message" to "Token is invalid or expired")
                )
            }
        }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception caught by StatusPages", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "type" to "https://trackly.com/errors/internal",
                    "title" to "Internal Server Error",
                    "status" to 500,
                    "detail" to (cause.localizedMessage ?: "An unexpected error occurred")
                )
            )
        }
    }

    val authService = AuthService()
    val orderService = OrderService()
    val driverService = DriverService()

    routing {
        get("/health") {
            call.respond(mapOf("status" to "UP", "app" to "Trackly Backend", "version" to "1.0.0"))
        }

        authRoutes(authService)
        orderRoutes(orderService)
        driverRoutes(driverService)
        trackingRoutes()
    }

    logger.info("Trackly Backend initialization complete.")
}
