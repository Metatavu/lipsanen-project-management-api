package fi.metatavu.lipsanen.milestones

import fi.metatavu.lipsanen.api.model.Milestone
import fi.metatavu.lipsanen.projects.ProjectController
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.tasks.TaskController
import fi.metatavu.lipsanen.tasks.TaskEntity
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.LocalDate
import java.util.*

/**
 * Controller for milestones
 */
@ApplicationScoped
class MilestoneController {

    @Inject
    lateinit var projectController: ProjectController

    @Inject
    lateinit var milestoneRepository: MilestoneRepository

    @Inject
    lateinit var taskController: TaskController

    /**
     * Lists milestones
     *
     * @param project project
     * @return list of milestones
     */
    suspend fun list(project: ProjectEntity): List<MilestoneEntity> {
        return milestoneRepository.list("project", Sort.by("startDate", Sort.Direction.Ascending), project)
            .awaitSuspending()
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
            originalStartDate = milestone.originalStartDate,
            originalEndDate = milestone.originalEndDate,
            estimatedReadiness = 0,
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
     * Finds a milestone
     *
     * @param milestoneId milestone id
     * @return found milestone or null if not found
     */
    suspend fun find(project: ProjectEntity, milestoneId: UUID): MilestoneEntity? {
        return find(milestoneId)?.takeIf { it.project.id == project.id }
    }

    /**
     * Updates a milestone
     *
     * @param existingMilestone existing milestone
     * @param updateData update data
     * @param milestoneTasks milestone tasks
     * @param isProjectPlanningStage is project in planning stage (affects original dates)
     * @param userId user id
     * @return updated milestone
     */
    suspend fun update(
        existingMilestone: MilestoneEntity,
        updateData: Milestone,
        milestoneTasks: List<TaskEntity>,
        userId: UUID,
        isProjectPlanningStage: Boolean = false
    ): MilestoneEntity {
        if (existingMilestone.startDate != updateData.startDate || existingMilestone.endDate != updateData.endDate) {
            updateTasksLengthWithinMilestone(existingMilestone, milestoneTasks, updateData.startDate, updateData.endDate, userId)
        }

        if (isProjectPlanningStage) {
            existingMilestone.originalStartDate = updateData.originalStartDate
            existingMilestone.originalEndDate = updateData.originalEndDate
        }

        existingMilestone.startDate = updateData.startDate
        existingMilestone.endDate = updateData.endDate
        existingMilestone.name = updateData.name
        existingMilestone.lastModifierId = userId
        return milestoneRepository.persistSuspending(existingMilestone)
    }

    /**
     * Deletes a milestone and dependent tasks
     *
     * @param milestone milestone
     */
    suspend fun delete(milestone: MilestoneEntity) {
        taskController.list(milestone).forEach {
            taskController.delete(it)
        }
        milestoneRepository.deleteSuspending(milestone)
    }

    /**
     * Do the checks if milestone dates can be adjusted, e.g.
     * if it is delayed it should not be delayed to later than any task end
     * if it is shortened it should not be shortened to earlier than any task start
     *
     * @param existingMilestone existing milestone
     * @param newMilestone new milestone
     * @param tasks tasks
     * @return string with detailed error or null if all is ok
     */
    fun checkForUpdateRestrictions(existingMilestone: MilestoneEntity, newMilestone: Milestone, tasks: List<TaskEntity>): String? {
        if (newMilestone.startDate.isAfter(existingMilestone.startDate)) {
            tasks.find { newMilestone.startDate.isAfter(it.endDate) }?.let {
                return "Milestone cannot be delayed to later than task ${it.name} end date"
            }
        }

        if (newMilestone.endDate.isBefore(existingMilestone.endDate)) {
            tasks.find { newMilestone.endDate.isBefore(it.startDate) }?.let {
                return "Milestone cannot be shortened to earlier than task ${it.name} start date"
            }
        }
        return null
    }

    /**
     * Updates task estimations so that they fit within the milestone and tasks are either extended or
     * shortened
     *
     * @param existingMilestone existing milestone
     * @param startDate new start date
     * @param endDate new end date
     * @param userId user id
     */
    private suspend fun updateTasksLengthWithinMilestone(
        existingMilestone: MilestoneEntity,
        milestoneTasks: List<TaskEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
        userId: UUID
    ) {
        if (milestoneTasks.isEmpty()) {
            return
        }
        val firstStartingTask = milestoneTasks[0]
        val lastEndingTask = milestoneTasks.maxBy { it.endDate }

        if (startDate.isBefore(existingMilestone.startDate)) {
            firstStartingTask.startDate = startDate
            firstStartingTask.lastModifierId = userId
            taskController.persist(firstStartingTask)
        } else if (startDate.isAfter(existingMilestone.startDate)) {
            milestoneTasks.filter { it.startDate < startDate }.forEach {
                it.startDate = startDate
                it.lastModifierId = userId
                taskController.persist(it)
            }
        }

        if (endDate.isAfter(existingMilestone.endDate)) {
            lastEndingTask.endDate = endDate
            lastEndingTask.lastModifierId = userId
            taskController.persist(lastEndingTask)
        } else if (endDate.isBefore(existingMilestone.endDate)) {
            milestoneTasks.filter { it.endDate > endDate }.forEach {
                it.endDate = endDate
                it.lastModifierId = userId
                taskController.persist(it)
            }
        }
    }
}
