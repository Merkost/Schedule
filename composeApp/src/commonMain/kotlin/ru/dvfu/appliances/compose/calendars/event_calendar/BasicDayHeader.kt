@file:Suppress("DEPRECATION")

package ru.dvfu.appliances.compose.calendars.event_calendar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn


@Composable
fun BasicDayHeader(
    day: LocalDate,
    modifier: Modifier = Modifier,
) {
    val currentDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    Text(
        text = formatDay(day),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        fontWeight = if (day.day == currentDate.day) FontWeight.ExtraBold else null
    )
}

@Preview(showBackground = true)
@Composable
fun BasicDayHeaderPreview() {
    BasicDayHeader(day = Clock.System.todayIn(TimeZone.currentSystemDefault()))
}
