package ru.dvfu.appliances.model.datasource.mock

import ru.dvfu.appliances.model.repository.entity.Appliance
import ru.dvfu.appliances.model.repository.entity.BookingStatus
import ru.dvfu.appliances.model.repository.entity.Event
import ru.dvfu.appliances.model.repository.entity.Roles
import ru.dvfu.appliances.model.repository.entity.User
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

object FakeData {

    val adminUser = User(
        userId = "admin-001",
        userName = "Админ Иванов",
        email = "admin@dvfu.ru",
        role = Roles.ADMIN.ordinal,
        anonymous = false,
        userPic = ""
    )

    val regularUser = User(
        userId = "user-001",
        userName = "Петр Сидоров",
        email = "sidorov@dvfu.ru",
        role = Roles.USER.ordinal,
        anonymous = false,
        userPic = ""
    )

    val secondUser = User(
        userId = "user-002",
        userName = "Мария Козлова",
        email = "kozlova@dvfu.ru",
        role = Roles.USER.ordinal,
        anonymous = false,
        userPic = ""
    )

    val guestUser = User(
        userId = "guest-001",
        userName = "Гость",
        email = "guest@dvfu.ru",
        role = Roles.GUEST.ordinal,
        anonymous = true,
        userPic = ""
    )

    val allUsers = listOf(adminUser, regularUser, secondUser, guestUser)

    val microscope = Appliance(
        id = "appliance-001",
        name = "Микроскоп JEOL",
        description = "Электронный микроскоп для лаборатории 301",
        color = 0xFF1976D2.toInt(),
        createdById = adminUser.userId,
        superuserIds = listOf(regularUser.userId),
        userIds = listOf(regularUser.userId, secondUser.userId),
        active = true
    )

    val spectrometer = Appliance(
        id = "appliance-002",
        name = "Спектрометр ИК",
        description = "Инфракрасный спектрометр Bruker",
        color = 0xFF388E3C.toInt(),
        createdById = adminUser.userId,
        superuserIds = listOf(secondUser.userId),
        userIds = listOf(regularUser.userId, secondUser.userId),
        active = true
    )

    val inactiveAppliance = Appliance(
        id = "appliance-003",
        name = "Центрифуга (на ремонте)",
        description = "Лабораторная центрифуга — временно недоступна",
        color = 0xFFF57C00.toInt(),
        createdById = adminUser.userId,
        superuserIds = emptyList(),
        userIds = listOf(regularUser.userId),
        active = false
    )

    val allAppliances = listOf(microscope, spectrometer, inactiveAppliance)

    fun generateEvents(baseDate: LocalDate = LocalDate.now()): List<Event> {
        val zone = ZoneId.systemDefault()

        fun toEpochMillis(date: LocalDate, time: LocalTime): Long =
            date.atTime(time).atZone(zone).toInstant().toEpochMilli()

        return listOf(
            Event(
                id = UUID.randomUUID().toString(),
                date = toEpochMillis(baseDate, LocalTime.MIDNIGHT),
                timeCreated = toEpochMillis(baseDate.minusDays(1), LocalTime.of(10, 0)),
                timeStart = toEpochMillis(baseDate, LocalTime.of(9, 0)),
                timeEnd = toEpochMillis(baseDate, LocalTime.of(11, 0)),
                commentary = "Анализ образцов серии А",
                applianceId = microscope.id,
                userId = regularUser.userId,
                status = BookingStatus.APPROVED,
                managedById = adminUser.userId,
                managedTime = toEpochMillis(baseDate.minusDays(1), LocalTime.of(12, 0)),
            ),
            Event(
                id = UUID.randomUUID().toString(),
                date = toEpochMillis(baseDate, LocalTime.MIDNIGHT),
                timeCreated = toEpochMillis(baseDate.minusDays(2), LocalTime.of(14, 0)),
                timeStart = toEpochMillis(baseDate, LocalTime.of(13, 0)),
                timeEnd = toEpochMillis(baseDate, LocalTime.of(15, 30)),
                commentary = "Спектральный анализ полимеров",
                applianceId = spectrometer.id,
                userId = secondUser.userId,
                status = BookingStatus.NONE,
            ),
            Event(
                id = UUID.randomUUID().toString(),
                date = toEpochMillis(baseDate.plusDays(1), LocalTime.MIDNIGHT),
                timeCreated = toEpochMillis(baseDate, LocalTime.of(8, 0)),
                timeStart = toEpochMillis(baseDate.plusDays(1), LocalTime.of(10, 0)),
                timeEnd = toEpochMillis(baseDate.plusDays(1), LocalTime.of(12, 0)),
                commentary = "Подготовка к конференции",
                applianceId = microscope.id,
                userId = adminUser.userId,
                status = BookingStatus.APPROVED,
                managedById = adminUser.userId,
                managedTime = toEpochMillis(baseDate, LocalTime.of(9, 0)),
            ),
            Event(
                id = UUID.randomUUID().toString(),
                date = toEpochMillis(baseDate.plusDays(2), LocalTime.MIDNIGHT),
                timeCreated = toEpochMillis(baseDate, LocalTime.of(16, 0)),
                timeStart = toEpochMillis(baseDate.plusDays(2), LocalTime.of(14, 0)),
                timeEnd = toEpochMillis(baseDate.plusDays(2), LocalTime.of(17, 0)),
                commentary = "Студенческая лабораторная",
                applianceId = spectrometer.id,
                userId = regularUser.userId,
                status = BookingStatus.DECLINED,
                managedById = secondUser.userId,
                managedTime = toEpochMillis(baseDate, LocalTime.of(17, 0)),
                managerCommentary = "Конфликт расписания"
            ),
        )
    }
}
