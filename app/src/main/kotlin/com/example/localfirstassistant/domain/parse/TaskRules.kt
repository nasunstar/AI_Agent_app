package com.example.localfirstassistant.domain.parse

/**
 * Regex heuristics for Korean/English temporal and action cues.
 * The score is based on coverage of date/time/verb/deadline groups.
 */
object TaskRules {
    val datePatterns = listOf(
        "오늘",
        "내일",
        "모레",
        "이번주",
        "이번 주",
        "이번달",
        "이번 달",
        "다음주",
        "다음 달",
        "\\b(today|tomorrow|next week|next month)\\b"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    val timePatterns = listOf(
        "\\b(\\d{1,2})시(\\d{1,2}분)?",
        "\\b(\\d{1,2}):(\\d{2})",
        "오전\\s*\\d{1,2}시",
        "오후\\s*\\d{1,2}시",
        "\\b(AM|PM)\\s*\\d{1,2}(:\\d{2})?"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    val verbPatterns = listOf(
        "확인",
        "검토",
        "보내",
        "답장",
        "신청",
        "제출",
        "request",
        "review",
        "reply",
        "submit"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    val deadlinePatterns = listOf(
        "마감",
        "까지",
        "due",
        "deadline"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    fun computeScore(text: String): Double {
        val normalized = text.lowercase()
        val hasDate = datePatterns.any { it.containsMatchIn(normalized) }
        val hasTime = timePatterns.any { it.containsMatchIn(normalized) }
        val hasVerb = verbPatterns.any { it.containsMatchIn(normalized) }
        val hasDeadline = deadlinePatterns.any { it.containsMatchIn(normalized) }

        val dateScore = if (hasDate) 0.4 else 0.0
        val timeScore = if (hasTime) 0.2 else 0.0
        val verbScore = if (hasVerb) 0.3 else 0.0
        val deadlineScore = if (hasDeadline) 0.1 else 0.0
        return dateScore + timeScore + verbScore + deadlineScore
    }
}
