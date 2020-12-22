package tables

import models.User
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import repo.ItemTable

class UserTable : ItemTable<User>() {
    val name = varchar("name",50)
    val email: Column<String> = varchar("email", 100).uniqueIndex()
    val password: Column<String> = varchar("password", 100)
    val active: Column<Boolean> = bool("active")

    override val primaryKey = PrimaryKey(id, name = "PK_USER_ID")

    override fun fill(builder: UpdateBuilder<Int>, item: User) {
        builder[name] = item.name
        builder[email] = item.email
        builder[password] = item.password
        builder[active] = item.active
    }
    override fun readResult(result: ResultRow) =
        User(
            result[id].value,
            result[name],
            result[email],
            result[password],
            result[active]
        )
}

val userTable = UserTable()
