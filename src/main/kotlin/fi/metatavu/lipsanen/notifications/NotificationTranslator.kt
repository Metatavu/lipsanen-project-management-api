package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.api.model.*
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
            notificationData = getNotificationData(entity)
        )
    }

    /**
     * Parses the data
     *
     * @param entity notification
     * @return data
     */
    fun getNotificationData(entity: NotificationEntity): Any {
        when (entity.type) {
            NotificationType.TASK_ASSIGNED -> {
                return TaskAssignedNotificationData(
                    taskId = entity.task!!.id,
                    taskName = entity.task!!.name
                )
            }
            NotificationType.TASK_STATUS_CHANGED -> {
                return TaskStatusChangesNotificationData(
                    taskId = entity.task!!.id,
                    taskName = entity.task!!.name,
                    newStatus = entity.task!!.status
                )
            }
            NotificationType.CHANGE_PROPOSAL_CREATED -> {
                return ChangeProposalCreatedNotificationData(
                    changeProposalId = entity.changeProposal!!.id,
                    taskId = entity.changeProposal!!.task!!.id,
                    taskName = entity.changeProposal!!.task!!.name,
                )
            }
            NotificationType.CHANGE_PROPOSAL_STATUS_CHANGED -> {
                return ChangeProposalStatusChangedNotificationData(
                    changeProposalId = entity.changeProposal!!.id,
                    taskId = entity.changeProposal!!.task!!.id,
                    taskName = entity.changeProposal!!.task!!.name,
                    newStatus = entity.changeProposal!!.status
                )
            }
            NotificationType.COMMENT_LEFT -> {
                return CommentLeftNotificationData(
                    taskId = entity.task!!.id,
                    taskName = entity.task!!.name,
                    commentId = entity.comment!!.id,
                    comment = entity.comment!!.comment
                )
            }
        }
    }
}