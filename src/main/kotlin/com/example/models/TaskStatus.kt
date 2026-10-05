package com.example.models

import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus {
    CREATED,
    RUNNING,
    COMPLETED,
    FAILED,
}