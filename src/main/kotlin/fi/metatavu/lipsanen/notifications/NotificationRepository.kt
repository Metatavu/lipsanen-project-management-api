package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.tasks.TaskEntity
import fi.metatavu.lipsanen.tasks.comments.TaskCommentEntity
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.time.OffsetDateTime
import java.util.*

/**
 * Repository for notifications
 */
@ApplicationScoped
class NotificationRepository : AbstractRepository<NotificationEntity, UUID>() {

    /**
     * Creates a new notification
     *
     * @param id notification id
     * @param type notification type
     * @param message notification message
     * @param task task
     * @param comment task comment
     * @return created notification
     */
    suspend fun create(
        id: UUID,
        type: NotificationType,
        message: String,
        task: TaskEntity? = null,
        comment: TaskCommentEntity? = null
    ): NotificationEntity {
        val notification = NotificationEntity()
        notification.id = id
        notification.type = type
        notification.message = message
        notification.task = task
        notification.comment = comment
        return persistSuspending(notification)
    }

    /**
     * Lists notifications created after a certain date
     *
     * @param createdBeforeFilter created before filter
     * @return list of notifications
     */
    suspend fun listCreatedBefore(createdBeforeFilter: OffsetDateTime): List<NotificationEntity> {
        return list("createdAt <= ?1", createdBeforeFilter).awaitSuspending()
    }
}