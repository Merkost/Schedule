package ru.dvfu.appliances.compose.utils

import com.mmk.kmpnotifier.notification.NotifierManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import ru.dvfu.appliances.AppDebug
import ru.dvfu.appliances.compose.use_cases.GetApplianceUseCase
import ru.dvfu.appliances.compose.use_cases.GetUserUseCase
import ru.dvfu.appliances.compose.viewmodels.EventDateAndTime
import ru.dvfu.appliances.model.datastore.UserDatastore
import ru.dvfu.appliances.model.repository.UsersRepository
import ru.dvfu.appliances.model.repository.entity.*
import ru.dvfu.appliances.model.repository.entity.notifications.Notification
import ru.dvfu.appliances.model.repository.entity.notifications.NotificationData
import ru.dvfu.appliances.model.repository.entity.notifications.PushNotification
import ru.dvfu.appliances.model.utils.*
import ru.dvfu.appliances.model.utils.Constants.NotificationType
import ru.dvfu.appliances.network.NotificationApi

class NotificationManagerImpl(
    private val userDatastore: UserDatastore,
    private val usersRepository: UsersRepository,
    private val getUserUseCase: GetUserUseCase,
    private val getApplianceUseCase: GetApplianceUseCase,
    private val notificationApi: NotificationApi,
) : NotificationManager {

    override suspend fun applianceDeleted(appliance: Appliance) {
        val currentUser = userDatastore.getCurrentUser.first()
        val users = (appliance.userIds + appliance.superuserIds)
            .mapNotNull { getUserUseCase(it).first().getOrNull() }
            .apply {
                if (AppDebug.isDebug.not())
                    filter { it.userId != currentUser.userId }
            }
            .map { it.msgToken }

        users.forEach {
            sendMessage(
                PushNotification(
                    to = it,
                    notification = Notification(
                        title = "Прибор \"${appliance.name}\" был удален",
                        body = "Также были отменены все бронирования на нем"
                    ),
                    data = NotificationData(NotificationType.APPLIANCE.name)
                )
            )
        }
    }

    override suspend fun eventUpdated(event: CalendarEvent, data: Map<String, Any?>) {
        if (userDatastore.getCurrentUser.first().userId != event.user.userId)
            sendMessage(
                PushNotification(
                    to = event.user.msgToken,
                    notification = Notification(
                        title = "Изменено бронирование на прибор \"${event.appliance.name}\"",
                        body = formattedDateTime(event.date, event.timeStart, event.timeEnd)
                                + ", ${event.status.getName().uppercase()}"
                    ),
                    data = NotificationData(NotificationType.MY_EVENT.name)
                )
            )
    }

    override suspend fun eventDeleted(event: CalendarEvent) {
        val currentUser = userDatastore.getCurrentUser.first()

        if (AppDebug.isDebug || currentUser.userId != event.user.userId) {
            sendMessage(
                PushNotification(
                    to = event.user.msgToken,
                    notification = Notification(
                        title = "Отменено бронирование на прибор \"${event.appliance.name}\"",
                        body = formattedDateTime(event.date, event.timeStart, event.timeEnd)
                    ),
                    data = NotificationData(NotificationType.MY_EVENT.name)
                )
            )
        }
    }

    override suspend fun newEvent(newEvent: Event) {
        val users = usersRepository.getUsers().first()
        val currentUser = userDatastore.getCurrentUser.first()
        getApplianceUseCase(newEvent.applianceId).first().getOrNull()?.let { appliance ->
            users.filter { appliance.superuserIds.contains(it.userId) }
                .apply { if (AppDebug.isDebug.not()) filter { it.userId != currentUser.userId } }
                .map { it.msgToken }
                .forEach {
                    sendMessage(
                        PushNotification(
                            to = it,
                            notification = Notification(
                                title = "Новое бронирование",
                                body = formattedApplianceDateTime(
                                    appliance.name,
                                    newEvent.date.toLocalDate(),
                                    newEvent.timeStart.toLocalDateTime(),
                                    newEvent.timeEnd.toLocalDateTime()
                                )
                            ),
                            data = NotificationData(NotificationType.NEW_EVENT.name)
                        )
                    )
                }
        }
    }

    override suspend fun newEventStatus(event: CalendarEvent, newStatus: BookingStatus) {
        sendMessage(
            PushNotification(
                to = event.user.msgToken,
                notification = Notification(
                    title = "Ваше бронирование ${newStatus.getName().uppercase()}",
                    body = formattedApplianceDateTimeStatus(
                        event.appliance.name,
                        date = event.date,
                        event.timeStart,
                        event.timeEnd,
                        status = newStatus
                    ),
                ),
                data = NotificationData(NotificationType.MY_EVENT.name)
            )
        )
    }

    override suspend fun eventTimeChanged(
        event: CalendarEvent,
        eventDateAndTime: EventDateAndTime
    ) {
        val currentUser = userDatastore.getCurrentUser.first()
        val sendTo = mutableListOf<String>()

        if (AppDebug.isDebug) {
            sendTo.add(event.user.msgToken)
            event.managedUser?.msgToken?.let { sendTo.add(it) }
        } else {
            if (currentUser.userId != event.user.userId) {
                sendTo.add(event.user.msgToken)
            }
            event.managedUser?.let {
                if (currentUser.userId != it.userId) {
                    sendTo.add(it.msgToken)
                }
            }
        }

        sendTo.forEach {
            sendMessage(
                PushNotification(
                    to = it,
                    notification = Notification(
                        title = "Изменено время бронирования",
                        body = formattedAppliance(event.appliance.name) + ", " +
                                formattedDateTimeStatus(
                                    event.date,
                                    event.timeStart,
                                    event.timeEnd,
                                    event.status
                                )
                    ),
                    data = NotificationData(
                        when (it) {
                            event.user.msgToken -> NotificationType.MY_EVENT.name
                            else -> NotificationType.EVENT.name
                        }
                    )
                )
            )
        }
    }

    override suspend fun newUserRole(user: User, role: Roles) {
        sendMessage(
            PushNotification(
                to = user.msgToken,
                notification = Notification(
                    title = "Ваша роль изменена",
                    body = "Теперь вы \"${org.jetbrains.compose.resources.getString(role.stringRes)}\""
                ),
                data = NotificationData(NotificationType.DEFAULT.name)
            )
        )
    }

    override suspend fun sendTestNotificationToCurrentDevice(): Result<String> = runCatching {
        val token = NotifierManager.getPushNotifier().getToken()
        check(!token.isNullOrBlank()) { "FCM token unavailable on this device" }
        notificationApi.postNotification(
            PushNotification(
                to = token,
                notification = Notification(
                    title = "Тестовое уведомление",
                    body = "Уведомления настроены и работают.",
                ),
                data = NotificationData(NotificationType.DEFAULT.name),
            ),
        )
        token
    }

    private suspend fun sendMessage(pushNotification: PushNotification) {
        notificationApi.postNotification(pushNotification)
    }

    suspend fun subscribeCurrentUser() {
        val currentUser = userDatastore.getCurrentUser.single()
        if (currentUser.isAnonymousOrGuest) return

        runCatching { NotifierManager.getPushNotifier().subscribeToTopic("weather") }
    }
}
