package fi.metatavu.lipsanen.projects.milestones.tasks.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposalStatus
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class ProposalRepository: AbstractRepository<ProposalEntity, UUID>() {

    suspend fun create(
        id: UUID,
        task: TaskEntity,
        reason: String?,
        comment: String?,
        status: ChangeProposalStatus,
        startDate: LocalDate?,
        endDate: LocalDate?,
        creatorId: UUID,
        lastModifierId: UUID
    ): ProposalEntity {
        val proposal = ProposalEntity()
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