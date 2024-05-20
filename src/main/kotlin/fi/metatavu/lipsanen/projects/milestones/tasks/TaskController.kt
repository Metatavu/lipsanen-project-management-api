package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.api.model.Task
import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.api.model.UserRole
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.projects.milestones.MilestoneEntity
import fi.metatavu.lipsanen.projects.milestones.tasks.connections.TaskConnectionController
import fi.metatavu.lipsanen.projects.milestones.tasks.proposals.ChangeProposalController
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

    @Inject
    lateinit var taskConnectionController: TaskConnectionController

    @Inject
    lateinit var proposalController: ChangeProposalController

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
            assigneeIds = task.assigneeIds ?: emptyList(),
            userRole = task.userRole ?: UserRole.USER,
            estimatedDuration = task.estimatedDuration ?: "",
            estimatedReadiness = task.estimatedReadiness ?: "",
            attachmentUrls = task.attachmentUrls ?: emptyList(),
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
        /*
        if the tasks extends beyond the milestone, the milestone is updated to fit that task
         */
        if (newTask.startDate < milestone.startDate) {
            milestone.startDate = newTask.startDate
        }
        if (newTask.endDate > milestone.endDate) {
            milestone.endDate = newTask.endDate
        }

        with(existingTask) {
            startDate = newTask.startDate
            endDate = newTask.endDate
            status = newTask.status
            name = newTask.name
            lastModifierId = userId
            assigneeIds = newTask.assigneeIds ?: emptyList()
            userRole = newTask.userRole ?: UserRole.USER
            estimatedDuration = newTask.estimatedDuration ?: ""
            estimatedReadiness = newTask.estimatedReadiness ?: ""
            attachmentUrls = newTask.attachmentUrls ?: emptyList()
        }
        return taskEntityRepository.persistSuspending(existingTask)
    }

    /**
     * Deletes a task and related entities
     *
     * @param foundTask task to delete
     */
    suspend fun delete(foundTask: TaskEntity) {
        taskConnectionController.list(foundTask).forEach {
            taskConnectionController.delete(it)
        }
        proposalController.list(foundTask).forEach {
            proposalController.delete(it)
        }
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