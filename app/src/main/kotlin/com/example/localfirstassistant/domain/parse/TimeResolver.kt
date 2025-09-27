package com.example.localfirstassistant.domain.parse

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.time.temporal.TemporalAdjusters

/**
 * Maps natural language time expressions to absolute timestamps in Asia/Seoul.
 */
object TimeResolver {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    private val weekdayMap = mapOf(
        "월" to DayOfWeek.MONDAY,
        "화" to DayOfWeek.TUESDAY,
        "수" to DayOfWeek.WEDNESDAY,
        "목" to DayOfWeek.THURSDAY,
        "금" to DayOfWeek.FRIDAY,
        "토" to DayOfWeek.SATURDAY,
        "일" to DayOfWeek.SUNDAY,
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY
    )

    fun resolve(text: String, reference: ZonedDateTime = ZonedDateTime.now(zone)): ZonedDateTime? {
        val normalized = text.lowercase(Locale.getDefault())
        var date = when {
            normalized.contains("오늘") || normalized.contains("today") -> reference.toLocalDate()
            normalized.contains("내일") || normalized.contains("tomorrow") -> reference.toLocalDate().plusDays(1)
            normalized.contains("모레") -> reference.toLocalDate().plusDays(2)
            normalized.contains("이번주") || normalized.contains("이번 주") || normalized.contains("this week") -> reference.toLocalDate()
            normalized.contains("다음주") || normalized.contains("다음 주") || normalized.contains("next week") -> reference.toLocalDate().plusWeeks(1)
            normalized.contains("이번달") || normalized.contains("이번 달") || normalized.contains("this month") -> reference.toLocalDate()
            normalized.contains("다음달") || normalized.contains("다음 달") || normalized.contains("next month") -> reference.toLocalDate().plusMonths(1).withDayOfMonth(1)
            else -> findWeekday(normalized, reference)
        }

        val time = parseTime(normalized) ?: LocalTime.of(9, 0)

        return date?.let { ZonedDateTime.of(it, time, zone) }
    }

    private fun findWeekday(text: String, reference: ZonedDateTime): LocalDate? {
        weekdayMap.entries.forEach { (key, day) ->
            if (text.contains(key)) {
                val candidate = reference.with(TemporalAdjusters.nextOrSame(day))
                return candidate.toLocalDate()
            }
        }
        val pattern = Regex("다음주\\s*([월화수목금토일])")
        val match = pattern.find(text)
        if (match != null) {
            val day = weekdayMap[match.groupValues[1]] ?: return null
            val candidate = reference.plusWeeks(1).with(TemporalAdjusters.nextOrSame(day))
            return candidate.toLocalDate()
        }
        val englishPattern = Regex("next week\\s*(monday|tuesday|wednesday|thursday|friday|saturday|sunday)")
        val englishMatch = englishPattern.find(text)
        if (englishMatch != null) {
            val day = weekdayMap[englishMatch.groupValues[1]] ?: return null
            val candidate = reference.plusWeeks(1).with(TemporalAdjusters.nextOrSame(day))
            return candidate.toLocalDate()
        }
        return null
    }

    private fun parseTime(text: String): LocalTime? {
        val colonPattern = Regex("(\\d{1,2}):(\\d{2})")
        colonPattern.find(text)?.let {
            val hour = it.groupValues[1].toInt()
            val minute = it.groupValues[2].toInt()
            return LocalTime.of(hour, minute)
        }
        val amPmPattern = Regex("(am|pm)\\s*(\\d{1,2})(:(\\d{2}))?")
        amPmPattern.find(text)?.let {
            var hour = it.groupValues[2].toInt()
            val minute = it.groupValues.getOrNull(4)?.takeIf { m -> m.isNotBlank() }?.toInt() ?: 0
            if (it.groupValues[1] == "pm" && hour != 12) hour += 12
            if (it.groupValues[1] == "am" && hour == 12) hour = 0
            return LocalTime.of(hour, minute)
        }
        val koreanPattern = Regex("(오전|오후)\\s*(\\d{1,2})시(\\d{1,2})?분?")
        koreanPattern.find(text)?.let {
            var hour = it.groupValues[2].toInt()
            val minute = it.groupValues.getOrNull(3)?.takeIf(String::isNotBlank)?.toIntOrNull() ?: 0
            val meridiem = it.groupValues[1]
            if (meridiem == "오후" && hour != 12) hour += 12
            if (meridiem == "오전" && hour == 12) hour = 0
            return LocalTime.of(hour, minute)
        }
        val hourPattern = Regex("(\\d{1,2})시")
        hourPattern.find(text)?.let {
            val hour = it.groupValues[1].toInt()
            return LocalTime.of(hour, 0)
        }
        return null
    }
}
