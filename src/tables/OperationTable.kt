package tables

import models.Operation
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import repo.ItemTable

class OperationTable : ItemTable<Operation>() {
    val name = varchar("name",50)


    override val primaryKey = PrimaryKey(id, name = "PK_OPERATION_ID")

    override fun fill(builder: UpdateBuilder<Int>, item: Operation) {
        builder[name] = item.name
    }
    override fun readResult(result: ResultRow) =
        Operation(
            result[id].value,
            result[name]
        )
}

val operationTable = OperationTable()

