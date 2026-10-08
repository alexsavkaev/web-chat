package chat.web.database

import javax.sql.DataSource
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.postgresql.ds.PGSimpleDataSource

data class DatabaseSettings(
    val jdbcUrl: String,
    val username: String,
    val password: String
)

fun databaseSettings(environment: Map<String, String>): DatabaseSettings = DatabaseSettings(
    jdbcUrl = environment["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5432/webchat",
    username = environment["DATABASE_USER"] ?: "webchat",
    password = environment["DATABASE_PASSWORD"] ?: "webchat"
)

fun createDataSource(settings: DatabaseSettings): DataSource = PGSimpleDataSource().apply {
    setURL(settings.jdbcUrl)
    user = settings.username
    password = settings.password
}

fun migrateDatabase(dataSource: DataSource) {
    dataSource.connection.use { connection ->
        val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
        database.defaultSchemaName = "webchat"
        Liquibase("db/changelog/db.changelog-master.yaml", ClassLoaderResourceAccessor(), database).update("")
    }
}
