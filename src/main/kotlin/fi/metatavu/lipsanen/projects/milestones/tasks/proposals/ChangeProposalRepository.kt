package fi.metatavu.lipsanen.projects.milestones.tasks.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposalStatus
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.*

/**
 * Repository for proposals
 */
@ApplicationScoped
class ChangeProposalRepository : AbstractRepository<ChangeProposalEntity, UUID>() {

    /**
     * Creates a new proposal
     *
     * @param id id
     * @param task task
     * @param reason reason
     * @param comment comment
     * @param status status
     * @param startDate start date
     * @param endDate end date
     * @param creatorId creator id
     * @param lastModifierId last modifier id
     * @return created proposal
     */
    suspend fun create(
        id: UUID,
        task: TaskEntity,
        reason: String,
        comment: String?,
        status: ChangeProposalStatus,
        startDate: LocalDate?,
        endDate: LocalDate?,
        creatorId: UUID,
        lastModifierId: UUID
    ): ChangeProposalEntity {
        val proposal = ChangeProposalEntity()
        proposal.id = id
        proposal.task = task
        proposal.reason = reason
        proposal.comment = comment
        proposal.status = status
        proposal.startDate = startDate
        proposal.endDate = endDate
        proposal.creatorId = creatorId
        proposal.lastModifierId = lastModifierId
        return persistSuspending(proposal)
    }
}