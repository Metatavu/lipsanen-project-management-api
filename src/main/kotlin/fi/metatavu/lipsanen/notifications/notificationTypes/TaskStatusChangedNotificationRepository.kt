package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.tasks.TaskEntity
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for task status changed notifications
 */
@ApplicationScoped
class TaskStatusChangedNotificationRepository : AbstractRepository<TaskStatusChangedNotificationData, UUID>() {

    /**
     * Creates a new task status changed notification
     *
     * @param id notification id
     * @param task task
     * @param taskName task name
     * @param status status
     * @param userId user id
     * @return created notification
     */
    suspend fun create(
        id: UUID,
        task: TaskEntity,
        taskName: String,
        status: TaskStatus,
        userId: UUID
    ): NotificationEntity {
        val notification = TaskStatusChangedNotificationData()
        notification.id = id
        notification.task = task
        notification.taskName = taskName
        notification.creatorId = userId
        notification.lastModifierId = userId

        notification.status = status
        return persistSuspending(notification)
    }

}