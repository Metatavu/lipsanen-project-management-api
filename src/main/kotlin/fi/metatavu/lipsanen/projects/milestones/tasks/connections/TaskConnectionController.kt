package fi.metatavu.lipsanen.projects.milestones.tasks.connections

import fi.metatavu.lipsanen.api.model.TaskConnection
import fi.metatavu.lipsanen.api.model.TaskConnectionRole
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.projects.milestones.MilestoneController
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskController
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for task connections
 */
@ApplicationScoped
class TaskConnectionController {

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var taskConnectionRepository: TaskConnectionRepository

    @Inject
    lateinit var milestoneController: MilestoneController

    /**
     * Lists task connections
     *
     * @param project project
     * @param task task
     * @param connectionRole connection role
     * @return list of task connections
     */
    suspend fun list(
        project: ProjectEntity,
        task: TaskEntity?,
        connectionRole: TaskConnectionRole?
    ): List<TaskConnectionEntity> {
        val tasksFilter = getTaskFilter(project, task)
        return if (connectionRole == null) {
            taskConnectionRepository.listByTasks(tasksFilter)
        } else {
            when (connectionRole) {
                TaskConnectionRole.SOURCE -> taskConnectionRepository.listBySourceTasks(tasksFilter)
                TaskConnectionRole.TARGET -> taskConnectionRepository.listByTargetTasks(tasksFilter)
            }
        }
    }

    /**
     * Creates a new task connection
     *
     * @param sourceTask source task
     * @param targetTask target task
     * @param taskConnection task connection
     * @param userId user id
     * @return created task connection
     */
    suspend fun create(
        sourceTask: TaskEntity,
        targetTask: TaskEntity,
        taskConnection: TaskConnection,
        userId: UUID
    ): TaskConnectionEntity {
        return taskConnectionRepository.create(
            id = UUID.randomUUID(),
            source = sourceTask,
            target = targetTask,
            type = taskConnection.type,
            creatorId = userId,
            lastModifierId = userId
        )
    }

    /**
     * Finds a task connection
     *
     * @param connectionId connection id
     * @param project project
     * @return found task connection or null if not found
     */
    suspend fun findById(connectionId: UUID, project: ProjectEntity): TaskConnectionEntity? {
        val projectMilestones = milestoneController.list(project)
        return taskConnectionRepository.find(
            "id = :connectionId and (source.milestone in :milestones or target.milestone in :milestones)",
            Parameters.with("connectionId", connectionId).and("milestones", projectMilestones)
        ).firstResult<TaskConnectionEntity>().awaitSuspending()
    }

    /**
     * Updates a task connection
     *
     * @param foundConnection found connection
     * @param sourceTask source task
     * @param targetTask target task
     * @param updatedTaskConnection updated task connection
     * @return updated task connection
     */
    suspend fun update(
        foundConnection: TaskConnectionEntity,
        sourceTask: TaskEntity,
        targetTask: TaskEntity,
        updatedTaskConnection: TaskConnection
    ): TaskConnectionEntity {
        foundConnection.source = sourceTask
        foundConnection.target = targetTask
        foundConnection.type = updatedTaskConnection.type
        return taskConnectionRepository.persistSuspending(foundConnection)
    }

    /**
     * Deletes a task connection
     *
     * @param foundConnection found connection
     */
    suspend fun delete(foundConnection: TaskConnectionEntity) {
        taskConnectionRepository.deleteSuspending(foundConnection)
    }

    /**
     * Gets the task filter based on project of provided task
     *
     * @param project project
     * @param task task
     * @return task filter
     */
    private suspend fun getTaskFilter(project: ProjectEntity, task: TaskEntity?): List<TaskEntity> {
        return if (task == null) {
            taskController.list(milestoneController.list(project))
        } else listOf(task)
    }

}