package com.bankBackend.models

import kotlinx.serialization.Serializable
import repo.Item

@Serializable
class Balance(
        override var id: Int = -1,
        var sum : Int,
        val name: String,
        val userId: Int
        ) : Item

@Serializable
class Balance_transfer (
        val id1 : Int,
        val id2 : Int,
        val sum : Int
        )