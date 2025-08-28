package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.api.model.*
import fi.metatavu.lipsanen.notifications.notificationTypes.TaskStatusChangedNotificationData
import fi.metatavu.lipsanen.rest.AbstractTranslator
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Translator for notifications
 */
@ApplicationScoped
class NotificationTranslator : AbstractTranslator<NotificationEntity, Notification>() {

    override suspend fun translate(entity: NotificationEntity): Notification {
        return Notification(
            id = entity.id,
            type = entity.notificationType,
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
        when (entity) {
            is fi.metatavu.lipsanen.notifications.notificationTypes.TaskAssignedNotificationData -> {
                return TaskAssignedNotificationData(
                    taskId = entity.task.id,
                    taskName = entity.taskName,
                    milestoneId = entity.task.milestone.id,
                    milestoneName = entity.task.milestone.name,
                    projectId = entity.task.milestone.project.id,
                    projectName = entity.task.milestone.project.name,
                    assigneeIds = entity.assigneeIds.split(",").mapNotNull {
                        try {
                            UUID.fromString(it)
                        } catch (_: Exception) {
                            null
                        }
                    }
                )
            }

            is TaskStatusChangedNotificationData -> {
                return TaskStatusChangesNotificationData(
                    taskId = entity.task.id,
                    taskName = entity.taskName,
                    newStatus = entity.status,
                    milestoneId = entity.task.milestone.id,
                    milestoneName = entity.task.milestone.name,
                    projectId = entity.task.milestone.project.id,
                    projectName = entity.task.milestone.project.name
                )
            }

            is fi.metatavu.lipsanen.notifications.notificationTypes.ChangeProposalCreatedNotificationData -> {
                return ChangeProposalCreatedNotificationData(
                    changeProposalId = entity.changeProposal!!.id,
                    taskId = entity.task.id,
                    taskName = entity.taskName,
                    milestoneId = entity.task.milestone.id,
                    milestoneName = entity.task.milestone.name,
                    projectId = entity.task.milestone.project.id,
                    projectName = entity.task.milestone.project.name
                )
            }

            is fi.metatavu.lipsanen.notifications.notificationTypes.ChangeProposalStatusChangedNotificationData -> {
                return ChangeProposalStatusChangedNotificationData(
                    changeProposalId = entity.changeProposal!!.id,
                    taskId = entity.task.id,
                    taskName = entity.taskName,
                    newStatus = entity.status,
                    milestoneId = entity.task.milestone.id,
                    milestoneName = entity.task.milestone.name,
                    projectId = entity.task.milestone.project.id,
                    projectName = entity.task.milestone.project.name
                )
            }

            is fi.metatavu.lipsanen.notifications.notificationTypes.CommentLeftNotificationData -> {
                return CommentLeftNotificationData(
                    taskId = entity.task.id,
                    taskName = entity.taskName,
                    commentId = entity.comment.id,
                    comment = entity.commentText,
                    milestoneId = entity.task.milestone.id,
                    milestoneName = entity.task.milestone.name,
                    projectId = entity.task.milestone.project.id,
                    projectName = entity.task.milestone.project.name
                )
            }

            else -> {
                return Any()
            }
        }
    }
}