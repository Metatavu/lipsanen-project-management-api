package fi.metatavu.lipsanen.projects.milestones

import fi.metatavu.lipsanen.api.model.Milestone
import fi.metatavu.lipsanen.projects.ProjectEntity
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for milestones
 */
@ApplicationScoped
class MilestoneController {

    @Inject
    lateinit var milestoneRepository: MilestoneRepository

    /**
     * Lists milestones
     *
     * @param project project
     * @return list of milestones
     */
    suspend fun list(project: ProjectEntity): List<MilestoneEntity> {
        return milestoneRepository.list("project", Sort.by("startDate", Sort.Direction.Ascending), project).awaitSuspending()

    }

    /**
     * Creates a new milestone
     *
     * @param project project
     * @param milestone milestone
     * @param userId user id
     * @return created milestone
     */
    suspend fun create(project: ProjectEntity, milestone: Milestone, userId: UUID): MilestoneEntity {
        return milestoneRepository.create(
            id = UUID.randomUUID(),
            project = project,
            name = milestone.name,
            startDate = milestone.startDate,
            endDate = milestone.endDate,
            creatorId = userId,
            lastModifierId = userId
        )
    }

    /**
     * Finds a milestone
     *
     * @param milestoneId milestone id
     * @return found milestone or null if not found
     */
    suspend fun find(milestoneId: UUID): MilestoneEntity? {
        return milestoneRepository.findByIdSuspending(milestoneId)
    }

    /**
     * Updates a milestone
     *
     * @param existingMilestone existing milestone
     * @param updateData update data
     * @param userId user id
     * @return updated milestone
     */
    suspend fun update(existingMilestone: MilestoneEntity, updateData: Milestone, userId: UUID): MilestoneEntity {
        existingMilestone.startDate = updateData.startDate
        existingMilestone.endDate = updateData.endDate
        existingMilestone.name = updateData.name
        existingMilestone.lastModifierId = userId
        return milestoneRepository.persistSuspending(existingMilestone)
    }

    /**
     * Deletes a milestone
     *
     * @param milestone milestone
     */
    suspend fun delete(milestone: MilestoneEntity) {
        milestoneRepository.deleteSuspending(milestone)
    }

    /**
     * Checks if milestone is part of the same project
     *
     * @param project project
     * @param milestone milestone
     * @return true if milestone is part of the same project
     */
    fun partOfSameProject(project: ProjectEntity, milestone: MilestoneEntity): Boolean {
        return project == milestone.project
    }

}