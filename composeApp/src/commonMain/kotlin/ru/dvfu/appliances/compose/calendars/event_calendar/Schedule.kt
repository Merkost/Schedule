@file:Suppress("DEPRECATION")

package ru.dvfu.appliances.compose.calendars.event_calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.dvfu.appliances.model.repository.entity.CalendarEvent
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun Schedule(
    calendarEvents: List<CalendarEvent>,
    modifier: Modifier = Modifier,
    onEventClick: (CalendarEvent) -> Unit,
    onEventLongClick: (CalendarEvent) -> Unit,
    eventContent: @Composable (positionedEvent: PositionedEvent) -> Unit =
        {
            BasicEvent(
                positionedEvent = it,
                onEventClick = onEventClick,
                onEventLongClick = onEventLongClick
            )
        },
    dayHeader: @Composable (day: LocalDate) -> Unit = { BasicDayHeader(day = it) },
    timeLabel: @Composable (time: LocalTime) -> Unit = { BasicSidebarLabel(time = it) },
    minDate: LocalDate = calendarEvents.minByOrNull(CalendarEvent::timeStart)?.timeStart?.date
        ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
    maxDate: LocalDate = calendarEvents.maxByOrNull(CalendarEvent::timeEnd)?.timeEnd?.date
        ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
    minTime: LocalTime = LocalTime(8, 0),
    maxTime: LocalTime = LocalTime(23, 0),
    daySize: ScheduleSize = ScheduleSize.Adaptive(256.dp),
    hourSize: ScheduleSize = ScheduleSize.Adaptive(64.dp),
    verticalScrollState: ScrollState,
    horizontalScrollState: ScrollState,
) {
    val numDays = minDate.daysUntil(maxDate) + 1
    val numMinutes = minutesBetween(minTime, maxTime).toInt() + 1
    val numHours = numMinutes.toFloat() / 60f
    var sidebarWidth by remember { mutableStateOf(0) }
    var headerHeight by remember { mutableStateOf(0) }
    BoxWithConstraints(modifier = modifier) {
        val dayWidth: Dp = when (daySize) {
            is ScheduleSize.FixedSize -> daySize.size
            is ScheduleSize.FixedCount -> with(LocalDensity.current) { ((constraints.maxWidth - sidebarWidth) / daySize.count).toDp() }
            is ScheduleSize.Adaptive -> with(LocalDensity.current) {
                maxOf(
                    ((constraints.maxWidth - sidebarWidth) / numDays).toDp(),
                    daySize.minSize
                )
            }
        }
        val hourHeight: Dp = when (hourSize) {
            is ScheduleSize.FixedSize -> hourSize.size
            is ScheduleSize.FixedCount -> with(LocalDensity.current) { ((constraints.maxHeight - headerHeight) / hourSize.count).toDp() }
            is ScheduleSize.Adaptive -> with(LocalDensity.current) {
                maxOf(
                    ((constraints.maxHeight - headerHeight) / numHours).toDp(),
                    hourSize.minSize
                )
            }
        }
        Column {
            ScheduleHeader(
                minDate = minDate,
                maxDate = maxDate,
                dayWidth = dayWidth,
                dayHeader = dayHeader,
                modifier = Modifier
                    .padding(start = with(LocalDensity.current) { sidebarWidth.toDp() })
                    .horizontalScroll(horizontalScrollState)
                    .onGloballyPositioned { headerHeight = it.size.height }
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Start)
            ) {
                ScheduleSidebar(
                    hourHeight = hourHeight,
                    minTime = minTime,
                    maxTime = maxTime,
                    label = timeLabel,
                    modifier = Modifier
                        .verticalScroll(verticalScrollState)
                        .onGloballyPositioned { sidebarWidth = it.size.width }
                )
                BasicSchedule(
                    calendarEvents = calendarEvents,
                    eventContent = eventContent,
                    minDate = minDate,
                    maxDate = maxDate,
                    minTime = minTime,
                    maxTime = maxTime,
                    dayWidth = dayWidth,
                    hourHeight = hourHeight,
                    verticalScrollState = verticalScrollState,
                    horizontalScrollState = horizontalScrollState,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                )
            }
        }
    }
}

@Composable
fun BasicSchedule(
    calendarEvents: List<CalendarEvent>,
    modifier: Modifier = Modifier,
    eventContent: @Composable (positionedEvent: PositionedEvent) -> Unit = {
        BasicEvent(
            positionedEvent = it,
            onEventClick = {},
            onEventLongClick = {})
    },
    minDate: LocalDate = calendarEvents.minByOrNull(CalendarEvent::timeStart)?.timeStart?.date
        ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
    maxDate: LocalDate = calendarEvents.maxByOrNull(CalendarEvent::timeEnd)?.timeEnd?.date
        ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
    minTime: LocalTime = LocalTime(0, 0),
    maxTime: LocalTime = LocalTime(23, 59, 59),
    dayWidth: Dp,
    hourHeight: Dp,
    verticalScrollState: ScrollState,
    horizontalScrollState: ScrollState,
) {
    val currentTime = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    }
    val currentDay = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var dayWidthForScroll = remember { 0 }
    var hourHeightForScroll = remember { 0 }

    val numDays = minDate.daysUntil(maxDate) + 1
    val numMinutes = minutesBetween(minTime, maxTime).toInt() + 1
    val numHours = numMinutes / 60
    val dividerColor = if (isSystemInDarkTheme()) Color.DarkGray else Color.LightGray
    val positionedEvents =
        remember(calendarEvents) { arrangeEvents(splitEvents(calendarEvents.sortedBy(CalendarEvent::timeStart))).filter { it.end > minTime && it.start < maxTime } }
    Layout(
        content = {
            positionedEvents.forEach { positionedEvent ->
                Box(modifier = Modifier.eventData(positionedEvent)) {
                    eventContent(positionedEvent)
                }
            }
        },
        modifier = modifier
            .drawBehind {
                val firstHour = minTime.truncatedToHours()
                val firstHourOffsetMinutes =
                    if (firstHour == minTime) 0L else minutesBetween(
                        minTime,
                        firstHour.plusHours(1)
                    )
                val firstHourOffset = (firstHourOffsetMinutes / 60f) * hourHeight.toPx()
                repeat(numHours) {
                    drawLine(
                        dividerColor,
                        start = Offset(0f, it * hourHeight.toPx() + firstHourOffset),
                        end = Offset(size.width, it * hourHeight.toPx() + firstHourOffset),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                repeat(numDays - 1) {
                    drawLine(
                        dividerColor,
                        start = Offset((it + 1) * dayWidth.toPx(), 0f),
                        end = Offset((it + 1) * dayWidth.toPx(), size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                drawLine(
                    Color.Blue,
                    start = Offset(
                        (currentDay.day - minDate.day) * dayWidth.toPx(),
                        (currentTime.hour - minTime.hour) * hourHeight.toPx() + firstHourOffset + (hourHeight.toPx() / 60f * currentTime.minute.toFloat())
                    ),
                    end = Offset(
                        (currentDay.day - minDate.day + 1) * dayWidth.toPx(),
                        (currentTime.hour - minTime.hour) * hourHeight.toPx() + firstHourOffset + (hourHeight.toPx() / 60f * currentTime.minute.toFloat())
                    ),
                    strokeWidth = 2.dp.toPx()
                )

            }
            .drawWithContent {
                this.drawContent()
                drawCircle(
                    Color.Blue,
                    center = Offset(
                        (currentDay.day - minDate.day) * dayWidth.toPx(),
                        (currentTime.hour - minTime.hour) * hourHeight.toPx() + (hourHeight.toPx() / 60f * currentTime.minute.toFloat())
                    ),
                    radius = 14f
                )

                drawCircle(
                    Color.Blue,
                    center = Offset(
                        (currentDay.day - minDate.day + 1) * dayWidth.toPx(),
                        (currentTime.hour - minTime.hour) * hourHeight.toPx() + (hourHeight.toPx() / 60f * currentTime.minute.toFloat())
                    ),
                    radius = 14f
                )
            }
    ) { measureables, constraints ->
        val height = (hourHeight.toPx() * (numMinutes / 60f)).roundToInt()
        val width = dayWidth.roundToPx() * numDays
        dayWidthForScroll = dayWidth.roundToPx()
        hourHeightForScroll = hourHeight.roundToPx()
        val placeablesWithEvents = measureables.map { measurable ->
            val splitEvent = measurable.parentData as PositionedEvent
            val eventDurationMinutes =
                minutesBetween(splitEvent.start, minOf(splitEvent.end, maxTime))
            val eventHeight = ((eventDurationMinutes / 60f) * hourHeight.toPx()).roundToInt()
            val eventWidth =
                ((splitEvent.colSpan.toFloat() / splitEvent.colTotal.toFloat()) * dayWidth.toPx()).roundToInt()
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = eventWidth,
                    maxWidth = eventWidth,
                    minHeight = eventHeight,
                    maxHeight = eventHeight
                )
            )
            Pair(placeable, splitEvent)
        }
        layout(width, height) {
            placeablesWithEvents.forEach { (placeable, splitEvent) ->
                val eventOffsetMinutes = if (splitEvent.start > minTime) minutesBetween(
                    minTime,
                    splitEvent.start
                ) else 0L
                val eventY = ((eventOffsetMinutes / 60f) * hourHeight.toPx()).roundToInt()
                val eventOffsetDays = minDate.daysUntil(splitEvent.date)
                val eventX =
                    eventOffsetDays * dayWidth.roundToPx() + (splitEvent.col * (dayWidth.toPx() / splitEvent.colTotal.toFloat())).roundToInt()
                placeable.place(eventX, eventY)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (verticalScrollState.value == 0) {
            verticalScrollState.animateScrollTo(((currentTime.hour - minTime.hour) * hourHeightForScroll))
            horizontalScrollState.animateScrollTo((((currentDay.day - minDate.day)) * dayWidthForScroll))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SchedulePreview() {
    //Schedule(sampleEvents, onEventClick = {}, onEventLongClick = {})
}