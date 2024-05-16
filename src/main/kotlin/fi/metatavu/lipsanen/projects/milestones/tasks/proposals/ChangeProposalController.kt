package fi.metatavu.lipsanen.projects.milestones.tasks.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposal
import fi.metatavu.lipsanen.api.model.ChangeProposalStatus
import fi.metatavu.lipsanen.projects.milestones.MilestoneEntity
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for proposals
 */
@ApplicationScoped
class ChangeProposalController {

    @Inject
    lateinit var proposalRepository: ChangeProposalRepository

    /**
     * Creates a new proposal
     *
     * @param task task
     * @param proposal proposal
     * @param creatorId creator id
     * @return created proposal
     */
    suspend fun create(
        task: TaskEntity,
        proposal: ChangeProposal,
        creatorId: UUID
    ): ChangeProposalEntity {
        return proposalRepository.create(
            id = UUID.randomUUID(),
            task = task,
            reason = proposal.reason,
            comment = proposal.comment,
            status = ChangeProposalStatus.PENDING,
            startDate = proposal.startDate,
            endDate = proposal.endDate,
            creatorId = creatorId,
            lastModifierId = creatorId
        )
    }

    /**
     * Lists change proposals
     *
     * @param task task
     * @param first first result
     * @param max max results
     * @return list of change proposals
     */
    suspend fun listChangeProposals(
        milestone: MilestoneEntity,
        taskFilter: TaskEntity?,
        first: Int?,
        max: Int?
    ): Pair<List<ChangeProposalEntity>, Long> {
        if (taskFilter != null) {
            return proposalRepository.applyFirstMaxToQuery(
                query = proposalRepository.find(
                    "task= :task",
                    Sort.ascending("createdAt"),
                    Parameters.with("task", taskFilter)
                ),
                firstIndex = first,
                maxResults = max
            )
        } else {
            return proposalRepository.applyFirstMaxToQuery(
                query = proposalRepository.find(
                    "task.milestone= :milestone",
                    Sort.ascending("createdAt"),
                    Parameters.with("milestone", milestone)
                ),
                firstIndex = first,
                maxResults = max
            )
        }
    }

    /**
     * List proposals for task
     *
     * @param task task
     * @return proposals for task
     */
    suspend fun list(task: TaskEntity): List<ChangeProposalEntity> {
        return proposalRepository.find("task=:task", Parameters.with("task", task)).list<ChangeProposalEntity>()
            .awaitSuspending()
    }

    /**
     * Finds a proposal for task
     *
     * @param task task
     * @param changeProposalId change proposal id
     * @return found proposal or null if not found
     */
    suspend fun find(task: TaskEntity, changeProposalId: UUID): ChangeProposalEntity? {
        return proposalRepository.find(
            "id=:id and task=:task",
            Parameters.with("task", task).and("id", changeProposalId)
        ).firstResult<ChangeProposalEntity>().awaitSuspending()
    }

    /**
     * Finds a proposal
     *
     * @param changeProposalId change proposal id
     * @return found proposal or null if not found
     */
    suspend fun find(changeProposalId: UUID): ChangeProposalEntity? {
        return proposalRepository.findByIdSuspending(changeProposalId)
    }

    /**
     * Updates a proposal
     *
     * @param foundProposal found proposal
     * @param changeProposal change proposal
     * @param userId user id
     * @return updated proposal
     */
    suspend fun update(
        foundProposal: ChangeProposalEntity,
        changeProposal: ChangeProposal,
        userId: UUID
    ): ChangeProposalEntity {
        foundProposal.reason = changeProposal.reason
        foundProposal.comment = changeProposal.comment
        foundProposal.startDate = changeProposal.startDate
        foundProposal.endDate = changeProposal.endDate
        foundProposal.lastModifierId = userId
        return proposalRepository.persistSuspending(foundProposal)
    }

    /**
     * Deletes a proposal
     *
     * @param proposal proposal
     */
    suspend fun delete(proposal: ChangeProposalEntity) {
        proposalRepository.deleteSuspending(proposal)
    }
}