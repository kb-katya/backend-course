package com.bankBackend.rest
import io.ktor.application.*
import io.ktor.routing.*
import kotlinx.serialization.KSerializer
import models.Operation
import repo.Repo

fun Application.restOperation(
    operationRepo: Repo<Operation>,
    operationSerializer: KSerializer<Operation>,
) {
    val path = "/operations"
    routing {

    }
}