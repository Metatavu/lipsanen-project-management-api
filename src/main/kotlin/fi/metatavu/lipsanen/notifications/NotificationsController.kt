package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.notifications.notificationevents.NotificationEventsController
import fi.metatavu.lipsanen.tasks.TaskEntity
import fi.metatavu.lipsanen.tasks.comments.TaskCommentEntity
import fi.metatavu.lipsanen.users.UserController
import fi.metatavu.lipsanen.users.UserEntity
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.scheduler.Scheduled
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Controller for notifications
 */
@ApplicationScoped
class NotificationsController {

    @Inject
    lateinit var notificationEventsController: NotificationEventsController

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var usersController: UserController

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var logger: org.jboss.logging.Logger

    @ConfigProperty(name = "notifications.cleanup.delay.days")
    var notificationCleanupDelayDays: Int? = null

    /**
     * Periodical job for cleaning the old notifications
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Scheduled(
        cron = "{cron.notifications.cleanup}", //0 0 0 * * ? every midnight
        delay = 15,
        delayUnit = TimeUnit.SECONDS
    )
    @WithTransaction
    fun clearOldNotifications() = CoroutineScope(vertx.dispatcher()).async {
        logger.info("Clearing old notifications (older than $notificationCleanupDelayDays days)")
        val createdBeforeFilter = OffsetDateTime.now().minusDays(notificationCleanupDelayDays!!.toLong())
        notificationRepository.listCreatedBefore(createdBeforeFilter).forEach {
            delete(it)
        }
    }.asUni().replaceWithVoid()

    /**
     * Lists notifications for a task
     *
     * @param task task
     * @return list of notifications
     */
    suspend fun list(task: TaskEntity): List<NotificationEntity> {
        return notificationRepository.list("task", task).awaitSuspending()
    }

    /**
     * Lists notifications for a comment
     *
     * @param comment task comment
     * @return list of notifications
     */
    suspend fun list(comment: TaskCommentEntity): List<NotificationEntity> {
        return notificationRepository.list("comment", comment).awaitSuspending()
    }

    /**
     *Creates a new notification and sends the notification events to the needed receivers
     * (e.g. admins + custom receivers)
     *
     * @param message notification message
     * @param type notification type
     * @param taskEntity task
     * @param comment task comment
     * @param receivers receivers
     * @param creatorId creator id
     * @return created notification
     */
    suspend fun createAndNotify(
        message: String,
        type: NotificationType,
        taskEntity: TaskEntity,
        receivers: List<UserEntity> = emptyList(),
        comment: TaskCommentEntity? = null,
        creatorId: UUID,
    ): NotificationEntity {
        val notification = notificationRepository.create(
            id = UUID.randomUUID(),
            type = type,
            message = message,
            task = taskEntity,
            comment = comment
        )

        val adminKeycloakIds = usersController.getAdmins().map { UUID.fromString(it.id) }
        val adminEntities = adminKeycloakIds.mapNotNull { usersController.findUser(it) }

        val notificationReceivers = (adminEntities + receivers).distinctBy { it.id }
        notificationReceivers.forEach { receiver ->
            notificationEventsController.create(
                notification = notification,
                receiver = receiver,
                creatorId = creatorId
            )
        }
        return notification
    }

    /**
     * Finds a notification by id
     *
     * @param notificationId notification id
     * @return found notification or null if not found
     */
    suspend fun find(notificationId: UUID): NotificationEntity? {
        return notificationRepository.findByIdSuspending(notificationId)
    }

    /**
     * Deletes a notification and connected notification events
     *
     * @param notification notification to delete
     */
    suspend fun delete(notification: NotificationEntity) {
        notificationEventsController.list(notification = notification).first.forEach {
            notificationEventsController.delete(it)
        }
        notificationRepository.deleteSuspending(notification)
    }
}