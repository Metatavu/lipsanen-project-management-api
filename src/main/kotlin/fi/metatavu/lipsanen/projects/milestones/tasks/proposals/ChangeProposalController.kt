package fi.metatavu.lipsanen.projects.milestones.tasks.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposal
import fi.metatavu.lipsanen.api.model.ChangeProposalStatus
import fi.metatavu.lipsanen.projects.milestones.MilestoneEntity
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskController
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

    @Inject
    lateinit var taskController: TaskController

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
     * @param milestone milestone
     * @param taskFilter task filter
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
     * List proposals for tasks
     *
     * @param tasks tasks
     * @return proposals for tasks
     */
    suspend fun list(tasks: List<TaskEntity>): List<ChangeProposalEntity> {
        return proposalRepository
            .find("task in :tasks", Parameters.with("tasks", tasks))
            .list<ChangeProposalEntity>()
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
     * @throws IllegalArgumentException
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

        if (foundProposal.status != changeProposal.status) {
            when (changeProposal.status) {
                ChangeProposalStatus.APPROVED -> {
                    foundProposal.status = changeProposal.status
                    approveChangeProposal(foundProposal, userId)
                }

                ChangeProposalStatus.REJECTED -> {
                    foundProposal.status = changeProposal.status
                }

                else -> {
                    // cannot change status to pending
                }
            }
        }
        return persist(foundProposal)
    }

    /**
     * Apply change proposal to the task:
     *  - change the task dates
     *  - find other proposals that got affected by this task's update and that belong to this task and auto-reject them
     *
     *  @param proposal found proposal
     *  @param userId user id
     *  @throws IllegalArgumentException if the proposal is not approved
     */
    private suspend fun approveChangeProposal(proposal: ChangeProposalEntity, userId: UUID) {
        // Update the task which will also do cascade updates and checks for milestone update validity
        taskController.update(
            existingTask = proposal.task,
            newStartDate = proposal.startDate ?: proposal.task.startDate,
            newEndDate = proposal.endDate ?: proposal.task.endDate,
            milestone = proposal.task.milestone,
            proposalMode = true,
            userId = userId
        )

        // reject all other proposals that affect the task
        list(proposal.task).forEach {
            if (it.id != proposal.id) {
                it.status = ChangeProposalStatus.REJECTED
                persist(it)
            }
        }
    }

    /**
     * Deletes a proposal
     *
     * @param proposal proposal
     */
    suspend fun delete(proposal: ChangeProposalEntity) {
        proposalRepository.deleteSuspending(proposal)
    }

    /**
     * Persists a proposal
     *
     * @param changeProposal change proposal
     */
    suspend fun persist(changeProposal: ChangeProposalEntity): ChangeProposalEntity {
        return proposalRepository.persistSuspending(changeProposal)
    }
}