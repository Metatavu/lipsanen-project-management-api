package fi.metatavu.lipsanen.tasks.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposalStatus
import fi.metatavu.lipsanen.milestones.MilestoneEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.tasks.TaskEntity
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
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

    /**
     * Lists proposals
     *
     * @param project project
     * @param milestoneFilter milestone filter
     * @param taskFilter task filter
     * @param first first
     * @param max max
     * @return pair of list of proposals and count
     */
    suspend fun list(
        project: ProjectEntity?,
        milestoneFilter: MilestoneEntity?,
        taskFilter: TaskEntity?,
        first: Int?,
        max: Int?
    ): Pair<List<ChangeProposalEntity>, Long> {
        val sb = StringBuilder()
        val parameters = Parameters()

        if(project != null) {
            addCondition(sb, "task.milestone.project= :project")
            parameters.and("project", project)
        }

        if (milestoneFilter != null) {
            addCondition(sb, " task.milestone= :milestone")
            parameters.and("milestone", milestoneFilter)
        }

        if (taskFilter != null) {
            addCondition(sb, "task= :task")
            parameters.and("task", taskFilter)
        }

        return applyFirstMaxToQuery(
            query = find(
                sb.toString(),
                Sort.ascending("createdAt"),
                parameters
            ),
            firstIndex = first,
            maxResults = max
        )
    }
}