package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.proposals.ChangeProposalEntity
import fi.metatavu.lipsanen.tasks.TaskEntity
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for change proposal created notifications
 */
@ApplicationScoped
class ChangeProposalCreatedNotificationRepository : AbstractRepository<ChangeProposalCreatedNotificationData, UUID>() {

    /**
     * Creates a new change proposal created notification
     *
     * @param id notification id
     * @param task task
     * @param taskName task name
     * @param changeProposal change proposal
     * @param userId user id
     * @return created notification
     */
    suspend fun create(
        id: UUID,
        task: TaskEntity,
        taskName: String,
        changeProposal: ChangeProposalEntity,
        userId: UUID
    ): NotificationEntity {
        val notification = ChangeProposalCreatedNotificationData()
        notification.id = id
        notification.task = task
        notification.taskName = taskName
        notification.creatorId = userId
        notification.lastModifierId = userId

        notification.changeProposal = changeProposal
        return persistSuspending(notification)
    }

    /**
     * Lists change proposal created notifications
     *
     * @param changeProposal change proposal
     * @return list of change proposal created notifications
     */
    suspend fun list(changeProposal: ChangeProposalEntity): List<ChangeProposalCreatedNotificationData> {
        return list("changeProposal", changeProposal).awaitSuspending()
    }
}