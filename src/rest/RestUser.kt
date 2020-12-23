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
import models.*
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
) {
    val path = "/users"
    routing {
        post("${path}/login") {
            val post = call.receive<AuthUserPayload>()
            val user = userRepo.read().find { it.email == post.email }
            if (user == null || !BCrypt.checkpw(post.password, user.password)) {
                call.respond(HttpStatusCode.BadRequest, "Email and password do not match")
            } else {
                call.respond(UserWithToken(simpleJwt.sign(user.email), user))
            }
        }
        post("${path}/registry") {
            parseBody(userSerializer)?.let { el ->
                if (userRepo.read().none { it.email == el.email }) {
                    val password = BCrypt.hashpw(el.password, BCrypt.gensalt())
                    if (userRepo.create(User(el.id, el.name, el.email, password, el.active))) {
                        userRepo.read().find { it.email == el.email }!!
                        call.respond(HttpStatusCode.Created)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.Conflict, "User already exists")
                }
            } ?: call.respond(HttpStatusCode.BadRequest)
        }
        authenticate {
            get("/user" ) {
                call.respond(HttpStatusCode.OK)
            }
            get("/user/{id}/balances") {
                parseId()?.let { id ->
                    balanceRepo.read().let { elem ->
                        call.respond(elem.filter { it.userId == id })
                    }
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
            get("/user/{id}/credits") {
                parseId()?.let { id ->
                    creditRepo.read().let { elem ->
                        call.respond(elem.filter { it.userId == id })
                    }
                } ?: call.respond(HttpStatusCode.BadRequest)
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
            put("/user/{id}/balance-transfer") {
                val post = call.receive<Balance_transfer>()
                val balance1 = balanceRepo.read().find { it.id == post.id1 }
                val balance2 = balanceRepo.read().find { it.id == post.id2 }
                if (balance1 != null && balance2 != null) {
                    if(balance1.sum >= post.sum) {
                        balance1.sum -= post.sum
                        balance2.sum += post.sum
                        balanceRepo.update(balance1.id, balance1)
                        balanceRepo.update(balance2.id, balance2)
                        call.respond(listOf(balance1, balance2))
                    }
                    else
                        call.respond(HttpStatusCode.BadRequest)
                } else {
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
                    val balance = balanceRepo.read().find { it.id == post.id1 }
                    val credit = creditRepo.read().find { it.id == post.id2 }
                    if (balance != null && credit != null) {
                        if(credit.balance >= post.sum) {
                            balance.sum += post.sum
                            credit.balance -= post.sum
                            creditRepo.update(credit.id, credit)
                            balanceRepo.update(balance.id, balance)
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
                val balance = balanceRepo.read().find { it.id == post.id1 }
                val credit = creditRepo.read().find { it.id == post.id2 }
                if (balance != null && credit != null) {
                    if (credit.balance >= post.sum) {
                        balance.sum -= post.sum
                        credit.sumCredit -= post.sum
                        creditRepo.update(credit.id,credit)
                        balanceRepo.update(balance.id, balance)
                        call.respond(HttpStatusCode.OK)
                    } else
                        call.respond(HttpStatusCode.BadRequest)
                } else {
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