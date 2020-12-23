package com.bankBackend.tables

import com.bankBackend.models.Balance
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import repo.ItemTable
import tables.userTable

class BalanceTable: ItemTable<Balance>() {
    val sum: Column<Int> = integer("sum2")
    val name = varchar("nameBalance", 50)
    val userId: Column<Int> = integer("userId").references(userTable.id)

    override val primaryKey = PrimaryKey(id, name = "PK_BALANCE_ID")

    override fun fill(builder: UpdateBuilder<Int>, item: Balance) {
        builder[name] = item.name
        builder[sum] = item.sum
        builder[userId] = item.userId
    }
    override fun readResult(result: ResultRow) =
            Balance(
                    result[id].value,
                    result[sum],
                    result[name],
                    result[userId]
            )
}

val balanceTable = BalanceTable()