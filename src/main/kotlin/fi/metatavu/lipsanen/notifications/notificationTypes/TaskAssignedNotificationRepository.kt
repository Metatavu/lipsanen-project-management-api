package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.tasks.TaskEntity
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for task assigned notifications
 */
@ApplicationScoped
class TaskAssignedNotificationRepository : AbstractRepository<TaskAssignedNotificationData, UUID>() {

    /**
     * Creates a new task assigned notification
     *
     * @param id notification id
     * @param task task
     * @param taskName task name
     * @param assigneeIds assignee ids
     * @param userId user id
     * @return created notification
     */
    suspend fun create(
        id: UUID,
        task: TaskEntity,
        taskName: String,
        assigneeIds: String,
        userId: UUID
    ): NotificationEntity {
        val notification = TaskAssignedNotificationData()
        notification.id = id
        notification.task = task
        notification.taskName = taskName
        notification.creatorId = userId
        notification.lastModifierId = userId

        notification.assigneeIds = assigneeIds
        return persistSuspending(notification)
    }

}