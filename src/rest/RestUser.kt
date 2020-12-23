package com.bankBackend.rest

import com.bankBackend.SimpleJWT
import com.bankBackend.models.Balance
import com.bankBackend.models.Balance_transfer
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
import models.Credit
import models.Operation
import models.User
import org.mindrot.jbcrypt.BCrypt
import repo.Repo

fun Application.restUser(
        simpleJwt: SimpleJWT,
        userRepo: Repo<User>,
        userSerializer: KSerializer<User>,
        balanceRepo: Repo<Balance>,
        balanceSerializer: KSerializer<Balance>,
        creditRepo: Repo<Credit>,
        creditSerializer: KSerializer<Credit>,
        operationRepo: Repo<Operation>,
        operationSerializer: KSerializer<Operation>
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
            route("/user/{id}/balance/{balanceID}") {
                get {
                    parseId("balanceID")?.let { id ->
                        balanceRepo.read(id)?.let { elem ->
                                call.respond(elem.sum)
                        }
                    }
                }
                post {
                    parseBody(balanceSerializer)?.let {
                        if (balanceRepo.create(it))
                            call.respond(HttpStatusCode.OK, "Balance created")
                        else
                            call.respond(HttpStatusCode.NotFound)
                    }?: call.respond(HttpStatusCode.BadRequest)
                }
                put {
                    parseBody(balanceSerializer)?.let {
                        parseId("balanceID")?.let { id ->
                            if (balanceRepo.update(id,it))
                                call.respond(HttpStatusCode.OK, "Balance updated")
                            else
                                call.respond(HttpStatusCode.NotFound)
                        }
                    }?: call.respond(HttpStatusCode.BadRequest)
                }
                }
            put("/user/{id}/balancetransf") {
                val post = call.receive<Balance_transfer>()
                val balan = balanceRepo.read().find { it.id == post.id1 }
                val balan1 = balanceRepo.read().find { it.id == post.id2 }
                if (balan != null && balan1 != null) {
                    if(balan.sum >= post.sum) {
                        balan.sum -= post.sum
                        balan1.sum += post.sum
                        balanceRepo.update(balan.id, balan)
                        balanceRepo.update(balan1.id,balan1)
                        call.respond(HttpStatusCode.OK)
                    }
                    else
                        call.respond(HttpStatusCode.BadRequest)
                }else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            route("/user/{id}/credit"){
                get {
                    parseId()?.let { id ->
                        creditRepo.read(id)?.let { elem ->
                            call.respond(elem.sumCredit)
                            call.respond(elem.balance)
                        }
                    }
                }
                post {
                    parseBody(creditSerializer)?.let { credit ->
                        if (creditRepo.create(credit)) {
                            call.respond(HttpStatusCode.OK, "Credit created")
                        }
                        else
                            call.respond(HttpStatusCode.NotFound)
                    }?: call.respond(HttpStatusCode.BadRequest)
                }
                put {
                    val post = call.receive<Balance_transfer>()
                    val balan = balanceRepo.read().find { it.id == post.id1 }
                    val balan1 = creditRepo.read().find { it.id == post.id2 }
                    if (balan != null && balan1 != null) {
                        if(balan1.balance >= post.sum) {
                            balan.sum += post.sum
                            balan1.balance -= post.sum
                            creditRepo.update(balan1.id,balan1)
                            balanceRepo.update(balan.id, balan)
                            call.respond(HttpStatusCode.OK)
                        }
                        else
                            call.respond(HttpStatusCode.BadRequest)
                    }else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
            put ("/user/{id}/payCredit") {
                val post = call.receive<Balance_transfer>()
                val balan = balanceRepo.read().find { it.id == post.id1 }
                val balan1 = creditRepo.read().find { it.id == post.id2 }
                if (balan != null && balan1 != null) {
                    if(balan1.balance >= post.sum) {
                        balan.sum -= post.sum
                        balan1.sumCredit -= post.sum
                        creditRepo.update(balan1.id,balan1)
                        balanceRepo.update(balan.id, balan)
                        call.respond(HttpStatusCode.OK)
                    }
                    else
                        call.respond(HttpStatusCode.BadRequest)
                }else {
                    call.respond(HttpStatusCode.NotFound)
                }
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