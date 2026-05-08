package ru.dvfu.appliances.compose.calendars.event_calendar

import androidx.compose.ui.unit.Dp
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

fun splitEvents(calendarEvents: List<CalendarEvent>): List<PositionedEvent> {
    return calendarEvents
        .map { event ->
            val startDate = event.timeStart.date
            val endDate = event.timeEnd.date
            if (startDate == endDate) {
                listOf(
                    PositionedEvent(
                        event,
                        SplitType.None,
                        startDate,
                        event.timeStart.time,
                        event.timeEnd.time,
                    )
                )
            } else {
                val days = startDate.daysUntil(endDate)
                val splitEvents = mutableListOf<PositionedEvent>()
                for (i in 0..days) {
                    val date = startDate.plus(i, DateTimeUnit.DAY)
                    splitEvents += PositionedEvent(
                        event,
                        splitType = if (date == startDate) SplitType.End else if (date == endDate) SplitType.Start else SplitType.Both,
                        date = date,
                        start = if (date == startDate) event.timeStart.time else LocalTime(0, 0),
                        end = if (date == endDate) event.timeEnd.time else LocalTime(23, 59, 59),
                    )
                }
                splitEvents
            }
        }
        .flatten()
}

fun PositionedEvent.overlapsWith(other: PositionedEvent): Boolean {
    return date == other.date && start < other.end && end > other.start
}

fun List<PositionedEvent>.timesOverlapWith(event: PositionedEvent): Boolean {
    return any { it.overlapsWith(event) }
}

fun arrangeEvents(events: List<PositionedEvent>): List<PositionedEvent> {
    val positionedEvents = mutableListOf<PositionedEvent>()
    val groupEvents: MutableList<MutableList<PositionedEvent>> = mutableListOf()

    fun resetGroup() {
        groupEvents.forEachIndexed { colIndex, col ->
            col.forEach { e ->
                positionedEvents.add(e.copy(col = colIndex, colTotal = groupEvents.size))
            }
        }
        groupEvents.clear()
    }

    events.forEach { event ->
        var firstFreeCol = -1
        var numFreeCol = 0
        for (i in 0 until groupEvents.size) {
            val col = groupEvents[i]
            if (col.timesOverlapWith(event)) {
                if (firstFreeCol < 0) continue else break
            }
            if (firstFreeCol < 0) firstFreeCol = i
            numFreeCol++
        }

        when {
            firstFreeCol < 0 -> {
                groupEvents += mutableListOf(event)
                for (ci in 0 until groupEvents.size - 1) {
                    val col = groupEvents[ci]
                    col.forEachIndexed { ei, e ->
                        if (ci + e.colSpan == groupEvents.size - 1 && !e.overlapsWith(event)) {
                            col[ei] = e.copy(colSpan = e.colSpan + 1)
                        }
                    }
                }
            }
            numFreeCol == groupEvents.size -> {
                resetGroup()
                groupEvents += mutableListOf(event)
            }
            else -> {
                groupEvents[firstFreeCol] += event.copy(colSpan = numFreeCol)
            }
        }
    }
    resetGroup()
    return positionedEvents
}

sealed class ScheduleSize {
    class FixedSize(val size: Dp) : ScheduleSize()
    class FixedCount(val count: Float) : ScheduleSize() {
        constructor(count: Int) : this(count.toFloat())
    }
    class Adaptive(val minSize: Dp) : ScheduleSize()
}
