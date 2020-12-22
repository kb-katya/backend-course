package com.bankBackend.rest

import com.bankBackend.SimpleJWT
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import models.AuthUserPayload
import models.User
import org.mindrot.jbcrypt.BCrypt
import repo.Repo

fun Application.restUser(
    simpleJwt: SimpleJWT,
    userRepo: Repo<User>,
    userSerializer: KSerializer<User>,
) {
    val path = "/users"
    routing {
        post("${path}/login") {
            val post = call.receive<AuthUserPayload>()
            val user = userRepo.read().find { it.email == post.email }
            if (user == null || !BCrypt.checkpw(post.password, user.password)) {
                call.respond(HttpStatusCode.BadRequest, "Email and password do not match")
            } else {
                call.respond(mapOf("token" to simpleJwt.sign(user.email)))
            }
        }
        post("${path}/registry") {
            parseBody(userSerializer)?.let { el ->
                if (userRepo.read().none { it.email == el.email }) {
                    val password = BCrypt.hashpw(el.password, BCrypt.gensalt())
                    if (userRepo.create(User(el.id, el.name, el.email, password, el.active))) {
                        val user = userRepo.read().find { it.email == el.email }!!
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Conflict, "User already exists")
                }
            } ?: call.respond(HttpStatusCode.BadRequest, "Parse error")
        }
        authenticate {
            get("/user" ) {
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

fun PipelineContext<Unit, ApplicationCall>.parseId(id: String = "id") =
    call.parameters[id]?.toIntOrNull()

suspend fun <T> PipelineContext<Unit, ApplicationCall>.parseBody(
    serializer: KSerializer<T>
) =
    try {
        Json.decodeFromString(
            serializer,
            call.receive()
        )
    } catch (e: Throwable) {
        null
    }