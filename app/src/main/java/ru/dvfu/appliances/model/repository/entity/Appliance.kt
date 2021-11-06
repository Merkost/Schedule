package ru.dvfu.appliances.model.repository.entity

import android.graphics.Color
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.dvfu.appliances.model.repository.entity.User
import ru.dvfu.appliances.model.utils.randomUUID
import java.util.*

@Parcelize
data class Appliance(
    val id: String = randomUUID(),
    val name: String = "",
    val description: String = "",
    val color: Int = Color.WHITE,
    val owner: String = "",
    val superuserIds: List<String> = listOf(),
    val userIds: List<String> = listOf()
): Parcelable
