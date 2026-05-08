package ru.dvfu.appliances.compose.calendars.event_calendar

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


@Composable
fun BasicSidebarLabel(
    time: LocalTime,
    modifier: Modifier = Modifier,
) {
    val currentTime = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    }
    Text(
        text = formatHour(time),
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp),
        color = if (time.hour == currentTime.hour) MaterialTheme.colorScheme.primary else Color.Unspecified,
        fontWeight = if (time.hour == currentTime.hour) FontWeight.ExtraBold else null
    )
}

@Preview(showBackground = true)
@Composable
fun BasicSidebarLabelPreview() {
    BasicSidebarLabel(time = LocalTime(13, 0), Modifier.sizeIn(maxHeight = 64.dp))
}
