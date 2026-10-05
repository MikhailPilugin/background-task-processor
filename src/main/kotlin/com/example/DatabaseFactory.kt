package com.example

import com.example.models.TasksTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    lateinit var database: Database
        private set

    private var dataSource: HikariDataSource? = null

    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:tasks;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }
        dataSource = HikariDataSource(config)
        database = Database.connect(dataSource!!)
        transaction(database) {
            SchemaUtils.create(TasksTable)
        }
    }

    fun close() {
        dataSource?.close()
        dataSource = null
    }
}