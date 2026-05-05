package ru.dvfu.appliances.model.repository.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import ru.dvfu.appliances.compose.ui.theme.Green500
import ru.dvfu.appliances.compose.ui.theme.Red500
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.approved_books
import ru.dvfu.appliances.generated.resources.declined_books
import ru.dvfu.appliances.generated.resources.new_books
import ru.dvfu.appliances.model.utils.StringOperation
import ru.dvfu.appliances.model.utils.randomUUID

@Serializable
data class Event(
    val id: String = randomUUID(),
    val date: Long = 0L,
    val timeCreated: Long = 0L,
    val timeStart: Long = 0L,
    val timeEnd: Long = 0L,
    val commentary: String = "0",
    val applianceId: String = "0",
    val userId: String = "0",
    val managedById: String? = null,
    val managedTime: Long? = null,
    val managerCommentary: String = "",
    val status: BookingStatus = BookingStatus.NONE,
)

@Serializable
enum class BookingStatus(
    override val stringRes: StringResource,
    val color: Color,
    val icon: ImageVector,
) : StringOperation {
    NONE(Res.string.new_books, Color.Unspecified, Icons.Default.HourglassBottom),
    APPROVED(Res.string.approved_books, Green500, Icons.Default.CheckCircle),
    DECLINED(Res.string.declined_books, Red500, Icons.Default.Cancel);

    fun getName() = when (this) {
        DECLINED -> "Отклонено"
        APPROVED -> "Подтверждено"
        NONE -> "На рассмотрении"
    }
}
