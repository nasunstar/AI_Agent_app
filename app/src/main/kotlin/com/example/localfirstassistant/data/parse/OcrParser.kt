package com.example.localfirstassistant.data.parse

import com.example.localfirstassistant.domain.parse.TaskRules
import com.example.localfirstassistant.domain.parse.TimeResolver
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Converts OCR extracted text into task candidates using shared parsing heuristics.
 */
object OcrParser {
    private val zone = ZoneId.of("Asia/Seoul")

    fun parse(text: String): TaskDraft {
        val lines = text.lines().filter { it.isNotBlank() }
        val title = lines.firstOrNull()?.trim()?.take(60) ?: "OCR Task"
        val description = text.trim().take(400)
        val resolvedTime = TimeResolver.resolve(text)
        val score = TaskRules.computeScore(text)
        val bucket = resolvedTime?.let { bucketFor(it) } ?: "MONTH"
        return TaskDraft(
            title = title,
            description = description,
            dueAt = resolvedTime,
            bucket = bucket,
            score = score
        )
    }

    private fun bucketFor(time: ZonedDateTime): String {
        val now = ZonedDateTime.now(zone)
        val days = Duration.between(now.toLocalDate().atStartOfDay(zone), time.toLocalDate().atStartOfDay(zone)).toDays()
        return when {
            days <= 0 -> "TODAY"
            days <= 7 -> "WEEK"
            else -> "MONTH"
        }
    }
}

data class TaskDraft(
    val title: String,
    val description: String,
    val dueAt: ZonedDateTime?,
    val bucket: String,
    val score: Double
)
