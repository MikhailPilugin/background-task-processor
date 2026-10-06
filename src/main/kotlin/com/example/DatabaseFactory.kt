package com.example

import com.example.models.TasksTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    lateinit var database: Database
        private set

    private var dataSource: HikariDataSource? = null

    fun init() {
        val jdbcUrl = System.getenv("JDBC_URL")
            ?: "jdbc:h2:mem:tasks;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
        val poolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 10

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            driverClassName = driverFor(jdbcUrl)
            maximumPoolSize = poolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }
        logger.info("Connecting to database: {}", jdbcUrl)

        val ds = HikariDataSource(config)
        dataSource = ds
        database = Database.connect(ds)
        transaction(database) {
            SchemaUtils.create(TasksTable)
        }
        logger.info("Database schema ready")
    }

    fun close() {
        dataSource?.close()
        dataSource = null
    }

    private fun driverFor(jdbcUrl: String): String = when {
        jdbcUrl.startsWith("jdbc:h2:") -> "org.h2.Driver"
        jdbcUrl.startsWith("jdbc:postgresql:") -> "org.postgresql.Driver"
        else -> throw IllegalArgumentException("Unsupported JDBC URL: $jdbcUrl")
    }
}
