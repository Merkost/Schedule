package ru.dvfu.appliances.compose.calendars

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
import ru.dvfu.appliances.model.utils.StringOperation

enum class CalendarType(override val stringRes: org.jetbrains.compose.resources.StringResource, val icon: ImageVector): StringOperation {
    WEEK(Res.string.week, Icons.Default.DateRange),
    MONTH(Res.string.month, Icons.Default.CalendarViewMonth),
    //THREE_DAYS(Res.string.three_days, Icons.Default.Today);
}