package models

import kotlinx.serialization.Serializable
import repo.Item

@Serializable
class Credit(
        override var id: Int = -1,
        var sumCredit: Int,
        var balance: Int,
        val name : String,
        val userId: Int
) : Item