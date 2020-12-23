package com.bankBackend
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.bankBackend.models.Balance
import com.bankBackend.rest.restUser
import com.bankBackend.tables.balanceTable
import com.bankBackend.tables.creditTable
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.auth.*
import config.DatabaseFactory
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.util.*
import models.Credit
import models.User
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import repo.RepoDSL
import tables.operationTable
import tables.userTable
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

class SimpleJWT(secret: String) {
    private val validityInMs = 36_000_00 * 1
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create()
        .withClaim("name", name)
        .withExpiresAt(getExpiration())
        .sign(algorithm)

    private fun getExpiration() = Date(System.currentTimeMillis() + validityInMs)
}

@KtorExperimentalAPI
@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val simpleJwt = SimpleJWT("BANK_BACKEND")

    install(ContentNegotiation) {
        json()
    }

    install(Authentication) {
        jwt {
            verifier(simpleJwt.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }

    DatabaseFactory.init()

    install(AutoHeadResponse)

    install(CORS) {
        header(HttpHeaders.ContentType)
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.AccessControlAllowHeaders)
        header(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
        anyHost()
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    restUser(
        simpleJwt,
        RepoDSL(userTable),
        User.serializer(),
        RepoDSL(balanceTable),
        Balance.serializer(),
        RepoDSL(creditTable),
        Credit.serializer(),
    )

    transaction {
        SchemaUtils.create(userTable)
        SchemaUtils.create(operationTable)
        SchemaUtils.create(balanceTable)
        SchemaUtils.create(creditTable)
    }

    routing {
        get("/api") {
            call.respondText("Version: 0.0.1", contentType = ContentType.Text.Plain)
        }
    }
}



