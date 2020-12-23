package models

import kotlinx.serialization.Serializable
import repo.Item

@Serializable
class User (
    override var id: Int = -1,
    val name: String,
    val email: String,
    var password: String,
    val active: Boolean = true
) : Item

@Serializable
data class AuthUserPayload(val email: String, val password: String)

@Serializable
data class UserWithToken(val token: String, val user: User)