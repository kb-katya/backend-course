package rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import repo.Item
import repo.Repo

fun <T : Item> Application.restRepo(
    repo: Repo<T>,
    path: String,
    serializer: KSerializer<T>
) {
    routing {
        route(path) {
            get {
                call.respond(repo.read())
            }
            post {
                call.respond(
                    parseBody(serializer)?.let { elem ->
                        if (repo.create(elem))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    } ?: HttpStatusCode.BadRequest
                )
            }
        }
        route("$path/{itemId}") {
            get {
                parseId()?.let { id ->
                    repo.read(id)?.let { elem ->
                        call.respond(elem)
                    } ?: call.respond(HttpStatusCode.NotFound)
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
            put {
                call.respond(
                    parseBody(serializer)?.let { elem ->
                        parseId()?.let { id ->
                            if (repo.update(id, elem))
                                HttpStatusCode.OK
                            else
                                HttpStatusCode.NotFound
                        }
                    } ?: HttpStatusCode.BadRequest
                )
            }
            delete {
                call.respond(
                    parseId()?.let { id ->
                        if (repo.delete(id))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    } ?: HttpStatusCode.BadRequest
                )
            }
        }
    }
}

fun PipelineContext<Unit, ApplicationCall>.parseId(id: String = "itemId") =
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