package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.api.model.Notification
import fi.metatavu.lipsanen.rest.AbstractTranslator
import jakarta.enterprise.context.ApplicationScoped

/**
 * Translator for notifications
 */
@ApplicationScoped
class NotificationTranslator : AbstractTranslator<NotificationEntity, Notification>() {

    override suspend fun translate(entity: NotificationEntity): Notification {
        return Notification(
            id = entity.id,
            type = entity.type,
            message = entity.message,
            taskId = entity.task?.id,
            commentId = entity.comment?.id,
        )
    }
}