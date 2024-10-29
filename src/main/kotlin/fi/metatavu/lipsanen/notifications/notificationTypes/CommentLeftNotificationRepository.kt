package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.tasks.TaskEntity
import fi.metatavu.lipsanen.tasks.comments.TaskCommentEntity
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for comment left notifications
 */
@ApplicationScoped
class CommentLeftNotificationRepository : AbstractRepository<CommentLeftNotificationData, UUID>() {

    /**
     * Creates a new comment left notification
     *
     * @param id notification id
     * @param task task
     * @param taskName task name
     * @param comment comment
     * @param commentText comment text
     * @param userId user id
     * @return created notification
     */
    suspend fun create(
        id: UUID,
        task: TaskEntity,
        taskName: String,
        comment: TaskCommentEntity,
        commentText: String,
        userId: UUID
    ): NotificationEntity {
        val notification = CommentLeftNotificationData()
        notification.id = id
        notification.task = task
        notification.taskName = taskName
        notification.creatorId = userId
        notification.lastModifierId = userId

        notification.comment = comment
        notification.commentText = commentText
        return persistSuspending(notification)
    }

}