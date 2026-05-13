package ru.dvfu.appliances.compose.viewmodels

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class EventDateAndTime(
    val date: LocalDate,
    val timeStart: LocalTime,
    val timeEnd: LocalTime,
)
