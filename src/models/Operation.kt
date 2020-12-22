package models

import kotlinx.serialization.Serializable
import repo.Item

@Serializable
class Operation(
    override var id: Int = -1,
    val name: String
) : Item