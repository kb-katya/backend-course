package com.bankBackend.tables

import models.Credit
import models.Operation
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import repo.ItemTable
import tables.userTable

class CreditTable: ItemTable<Credit>() {
    val sumCredit: Column<Int> = integer("sum1")
    val name = varchar("nameCredit", 50)
    val balance: Column<Int> = integer("balance")
    val userId: Column<Int> = integer("userId").references(userTable.id)

    override val primaryKey = PrimaryKey(id, name = "PK_CREDIT_ID")

    override fun fill(builder: UpdateBuilder<Int>, item: Credit) {
        builder[name] = item.name
        builder[sumCredit] = item.sumCredit
        builder[balance] = item.balance
        builder[userId] = item.userId
    }
    override fun readResult(result: ResultRow) =
            Credit(
                  result[id].value,
                  result[sumCredit],
                  result[balance],
                  result[name],
                    result[userId]
            )
}

val creditTable = CreditTable()
