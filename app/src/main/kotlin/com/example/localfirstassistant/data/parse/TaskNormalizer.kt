package com.example.localfirstassistant.data.parse

import com.example.localfirstassistant.data.db.entities.TaskEntity
import com.example.localfirstassistant.data.db.entities.TaskSource
import com.example.localfirstassistant.data.db.entities.TaskStatus
import com.example.localfirstassistant.domain.parse.TaskRules
import com.example.localfirstassistant.domain.parse.TimeResolver
import java.time.Duration
import java.time.Instant

/**
 * Shared helpers to convert arbitrary text into TaskEntity.
 */
object TaskNormalizer {
    fun normalize(
        title: String,
        body: String,
        source: TaskSource,
        reference: Instant = Instant.now(),
        resolvedInstant: Instant? = null,
        scoreOverride: Double? = null,
        bucketOverride: String? = null
    ): TaskEntity {
        val text = "$title\n$body"
        val score = scoreOverride ?: TaskRules.computeScore(text)
        val resolved = resolvedInstant ?: TimeResolver.resolve(text)?.toInstant()
        val bucket = bucketOverride ?: resolved?.let { bucketFor(it, reference) } ?: bucketFor(reference, reference)
        val status = statusForScore(score)
        return TaskEntity(
            title = title.take(80),
            description = body.take(4000),
            dueAt = resolved,
            dueBucket = bucket,
            score = score,
            status = status,
            source = source,
            createdAt = reference,
            updatedAt = Instant.now()
        )
    }

    fun statusForScore(score: Double): TaskStatus = when {
        score >= 0.75 -> TaskStatus.PENDING
        score >= 0.5 -> TaskStatus.REVIEW
        else -> TaskStatus.SNOOZED
    }

    fun bucketFor(instant: Instant, reference: Instant): String {
        val diffDays = Duration.between(reference, instant).toDays()
        return when {
            diffDays <= 0 -> "TODAY"
            diffDays <= 7 -> "WEEK"
            else -> "MONTH"
        }
    }
}
