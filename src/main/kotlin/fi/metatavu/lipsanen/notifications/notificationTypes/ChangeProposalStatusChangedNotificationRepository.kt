package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.api.model.ChangeProposalStatus
import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.proposals.ChangeProposalEntity
import fi.metatavu.lipsanen.tasks.TaskEntity
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for change proposal status changed notifications
 */
@ApplicationScoped
class ChangeProposalStatusChangedNotificationRepository :
    AbstractRepository<ChangeProposalStatusChangedNotificationData, UUID>() {

    /**
     * Creates a new change proposal status changed notification
     *
     * @param id notification id
     * @param task task
     * @param taskName task name
     * @param changeProposal change proposal
     * @param status status
     * @param userId user id
     * @return created notification
     */
    suspend fun create(
        id: UUID,
        task: TaskEntity,
        taskName: String,
        changeProposal: ChangeProposalEntity,
        status: ChangeProposalStatus,
        userId: UUID
    ): NotificationEntity {
        val notification = ChangeProposalStatusChangedNotificationData()
        notification.id = id
        notification.task = task
        notification.taskName = taskName
        notification.creatorId = userId
        notification.lastModifierId = userId

        notification.changeProposal = changeProposal
        notification.status = status
        return persistSuspending(notification)
    }

    /**
     * Lists change proposal status changed notifications
     *
     * @param changeProposal change proposal
     * @return list of change proposal status changed notifications
     */
    suspend fun list(changeProposal: ChangeProposalEntity): List<ChangeProposalStatusChangedNotificationData> {
        return list("changeProposal", changeProposal).awaitSuspending()
    }
}