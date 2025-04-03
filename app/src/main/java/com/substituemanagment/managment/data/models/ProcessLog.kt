package com.substituemanagment.managment.data.models

data class ProcessLog(
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val details: String,
    val status: LogStatus = LogStatus.INFO,
    val data: Map<String, Any>? = null
)

enum class LogStatus {
    INFO,
    WARNING,
    ERROR
} 