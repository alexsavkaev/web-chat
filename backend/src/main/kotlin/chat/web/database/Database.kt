package chat.web.database

import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource
import org.flywaydb.core.Flyway
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

fun interface SqlConnectionFactory {
    fun open(): Connection
}

fun jdbcConnectionFactory(settings: DatabaseSettings): SqlConnectionFactory = SqlConnectionFactory {
    DriverManager.getConnection(settings.jdbcUrl, settings.username, settings.password)
}

fun migrateDatabase(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .schemas("webchat")
        .defaultSchema("webchat")
        .locations("classpath:db/migration")
        .load()
        .migrate()
}
