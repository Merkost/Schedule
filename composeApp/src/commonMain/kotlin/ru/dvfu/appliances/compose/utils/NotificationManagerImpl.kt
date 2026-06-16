package ru.dvfu.appliances.compose.utils

import com.mmk.kmpnotifier.notification.NotifierManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
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
    private val appScope: CoroutineScope,
) : NotificationManager {

    override suspend fun applianceDeleted(appliance: Appliance) {
        appScope.launch {
            val currentUser = userDatastore.getCurrentUser.first()
            val users = (appliance.userIds + appliance.superuserIds)
                .mapNotNull { getUserUseCase(it).first().getOrNull() }
            val recipients = if (AppDebug.isDebug) users else users.filter { it.userId != currentUser.userId }
            fanout(
                tokens = recipients.map { it.msgToken },
                title = "Прибор \"${appliance.name}\" был удален",
                body = "Также были отменены все бронирования на нем",
                type = NotificationType.APPLIANCE,
            )
        }
    }

    override suspend fun eventUpdated(event: CalendarEvent, data: Map<String, Any?>) {
        appScope.launch {
            if (userDatastore.getCurrentUser.first().userId == event.user.userId) return@launch
            fanout(
                tokens = listOf(event.user.msgToken),
                title = "Изменено бронирование на прибор \"${event.appliance.name}\"",
                body = formattedDateTime(event.date, event.timeStart, event.timeEnd) +
                    ", ${event.status.getName().uppercase()}",
                type = NotificationType.MY_EVENT,
            )
        }
    }

    override suspend fun eventDeleted(event: CalendarEvent) {
        appScope.launch {
            val currentUser = userDatastore.getCurrentUser.first()
            if (!AppDebug.isDebug && currentUser.userId == event.user.userId) return@launch
            fanout(
                tokens = listOf(event.user.msgToken),
                title = "Отменено бронирование на прибор \"${event.appliance.name}\"",
                body = formattedDateTime(event.date, event.timeStart, event.timeEnd),
                type = NotificationType.MY_EVENT,
            )
        }
    }

    override suspend fun newEvent(newEvent: Event) {
        appScope.launch {
            val users = usersRepository.getUsers().first()
            val currentUser = userDatastore.getCurrentUser.first()
            val appliance = getApplianceUseCase(newEvent.applianceId).first().getOrNull() ?: return@launch
            val superusers = users.filter { appliance.superuserIds.contains(it.userId) }
            val recipients = if (AppDebug.isDebug) superusers else superusers.filter { it.userId != currentUser.userId }
            fanout(
                tokens = recipients.map { it.msgToken },
                title = "Новое бронирование",
                body = formattedApplianceDateTime(
                    appliance.name,
                    newEvent.date.toLocalDate(),
                    newEvent.timeStart.toLocalDateTime(),
                    newEvent.timeEnd.toLocalDateTime(),
                ),
                type = NotificationType.NEW_EVENT,
            )
        }
    }

    override suspend fun newEventStatus(event: CalendarEvent, newStatus: BookingStatus) {
        appScope.launch {
            fanout(
                tokens = listOf(event.user.msgToken),
                title = "Ваше бронирование ${newStatus.getName().uppercase()}",
                body = formattedApplianceDateTimeStatus(
                    event.appliance.name,
                    date = event.date,
                    event.timeStart,
                    event.timeEnd,
                    status = newStatus,
                ),
                type = NotificationType.MY_EVENT,
            )
        }
    }

    override suspend fun eventTimeChanged(event: CalendarEvent, eventDateAndTime: EventDateAndTime) {
        appScope.launch {
            val currentUser = userDatastore.getCurrentUser.first()
            val ownerTokens = if (AppDebug.isDebug || currentUser.userId != event.user.userId) {
                listOf(event.user.msgToken)
            } else emptyList()
            val managerTokens = event.managedUser
                ?.takeIf { AppDebug.isDebug || currentUser.userId != it.userId }
                ?.let { listOf(it.msgToken) }
                ?: emptyList()
            val body = formattedAppliance(event.appliance.name) + ", " +
                formattedDateTimeStatus(event.date, event.timeStart, event.timeEnd, event.status)
            fanout(
                tokens = ownerTokens,
                title = "Изменено время бронирования",
                body = body,
                type = NotificationType.MY_EVENT,
            )
            fanout(
                tokens = managerTokens,
                title = "Изменено время бронирования",
                body = body,
                type = NotificationType.EVENT,
            )
        }
    }

    override suspend fun newUserRole(user: User, role: Roles) {
        appScope.launch {
            fanout(
                tokens = listOf(user.msgToken),
                title = "Ваша роль изменена",
                body = "Теперь вы \"${org.jetbrains.compose.resources.getString(role.stringRes)}\"",
                type = NotificationType.DEFAULT,
            )
        }
    }

    override suspend fun sendTestNotificationToCurrentDevice(): Result<String> = runCatching {
        val log = org.kimplify.cedar.Cedar.tag("FCM")
        val token = NotifierManager.getPushNotifier().getToken()
        val authUser = Firebase.auth.currentUser
        log.d("test push: token=${token?.take(12)}…(len=${token?.length}) authUid=${authUser?.uid} anon=${authUser?.isAnonymous}")
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
        log.d("test push: posted ok")
        token
    }

    private suspend fun fanout(tokens: List<String>, title: String, body: String, type: NotificationType) {
        tokens.filter { it.isNotBlank() }.distinct().forEach { token ->
            runCatching {
                notificationApi.postNotification(
                    PushNotification(
                        to = token,
                        notification = Notification(title, body),
                        data = NotificationData(type.name),
                    ),
                )
            }
        }
    }

    suspend fun subscribeCurrentUser() {
        val currentUser = userDatastore.getCurrentUser.single()
        if (currentUser.isAnonymousOrGuest) return
        runCatching { NotifierManager.getPushNotifier().subscribeToTopic("weather") }
    }
}
