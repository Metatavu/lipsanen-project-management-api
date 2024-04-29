package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.api.model.Task
import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.projects.milestones.MilestoneEntity
import fi.metatavu.lipsanen.projects.milestones.tasks.connections.TaskConnectionController
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for tasks
 */
@ApplicationScoped
class TaskController {

    @Inject
    lateinit var taskEntityRepository: TaskEntityRepository

    /**
     * Lists tasks
     *
     * @param milestone milestone
     * @param first first result
     * @param max max results
     * @return list of tasks
     */
    suspend fun list(milestone: MilestoneEntity, first: Int?, max: Int?): Pair<List<TaskEntity>, Long> {
        return taskEntityRepository.applyFirstMaxToQuery(
            query = taskEntityRepository.find("milestone", Sort.ascending("startDate"), milestone),
            firstIndex = first,
            maxResults = max
        )
    }

    /**
     * Lists tasks
     *
     * @param milestone milestone
     * @return list of tasks
     */
    suspend fun list(milestone: MilestoneEntity): List<TaskEntity> {
        return taskEntityRepository.find("milestone", milestone).list<TaskEntity>().awaitSuspending()
    }

    /**
     * Lists tasks
     *
     * @param project project
     * @return list of tasks
     */
    suspend fun list(milestones: List<MilestoneEntity>): List<TaskEntity> {
        return taskEntityRepository.find("milestone in :milestones", Parameters().and("milestones", milestones)).list<TaskEntity>().awaitSuspending()
    }

    /**
     * Creates a new task
     *
     * @param milestone milestone
     * @param task task
     * @param userId user id
     * @return created task
     */
    suspend fun create(milestone: MilestoneEntity, task: Task, userId: UUID): TaskEntity {
        return taskEntityRepository.create(
            id = UUID.randomUUID(),
            name = task.name,
            startDate = task.startDate,
            endDate = task.endDate,
            milestone = milestone,
            status = TaskStatus.NOT_STARTED,
            creatorId = userId,
            lastModifierId = userId
        )
    }

    /**
     * Finds a task within a milestone
     *
     * @param milestone milestone
     * @param taskId task id
     * @return found task or null if not found
     */
    suspend fun find(milestone: MilestoneEntity, taskId: UUID): TaskEntity? {
        return taskEntityRepository.find(
            "milestone = :milestone and id = :id",
            Parameters.with("milestone", milestone).and("id", taskId)
        ).firstResult<TaskEntity?>().awaitSuspending()
    }

    /**
     * Finds a task within a project
     *
     * @param project project
     * @param taskId task id
     * @return found task or null if not found
     */
    suspend fun find(project: ProjectEntity, taskId: UUID): TaskEntity? {
        return taskEntityRepository.find(
            "milestone.project = :project and id = :id",
            Parameters.with("project", project).and("id", taskId)
        ).firstResult<TaskEntity?>().awaitSuspending()
    }

    /**
     * Updates a task
     *
     * @param existingTask existing task
     * @param newTask new task
     * @param milestone milestone
     * @param userId user id
     * @return updated task
     */
    suspend fun update(
        existingTask: TaskEntity,
        newTask: Task,
        milestone: MilestoneEntity,
        userId: UUID
    ): TaskEntity {
        if (newTask.startDate != existingTask.startDate || newTask.endDate != existingTask.endDate) {
            if (newTask.startDate < milestone.startDate || newTask.endDate > milestone.endDate) {
                if (newTask.startDate < milestone.startDate) {
                    existingTask.startDate = milestone.startDate
                }
                if (newTask.endDate > milestone.endDate) {
                    existingTask.endDate = milestone.endDate
                }
            }
        }

        existingTask.status = newTask.status    // verification if updates are required is done in the api impl
        existingTask.name = newTask.name
        existingTask.lastModifierId = userId
        return taskEntityRepository.persistSuspending(existingTask)
    }

    /**
     * Deletes a task
     *
     * @param foundTask task to delete
     */
    suspend fun delete(foundTask: TaskEntity) {
        taskEntityRepository.deleteSuspending(foundTask)
    }

    /**
     * Persists a task
     *
     * @param startsFirst task to persist
     * @return persisted task
     */
    suspend fun persist(startsFirst: TaskEntity): TaskEntity {
        return taskEntityRepository.persistSuspending(startsFirst)
    }

}