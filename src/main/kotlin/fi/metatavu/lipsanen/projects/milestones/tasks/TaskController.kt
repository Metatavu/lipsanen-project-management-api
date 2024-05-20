package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.api.model.*
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.projects.milestones.MilestoneEntity
import fi.metatavu.lipsanen.projects.milestones.tasks.connections.TaskConnectionController
import fi.metatavu.lipsanen.projects.milestones.tasks.proposals.ChangeProposalController
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.LocalDate
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
        return taskEntityRepository.find("milestone in :milestones", Parameters().and("milestones", milestones))
            .list<TaskEntity>().awaitSuspending()
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
     * @throws IllegalArgumentException
     */
    suspend fun update(
        existingTask: TaskEntity,
        newTask: Task,
        milestone: MilestoneEntity,
        userId: UUID
    ): TaskEntity {
        //if the task extends beyond the milestone, the milestone is updated to fit that task
        if (newTask.startDate < milestone.startDate) {
            milestone.startDate = newTask.startDate
        }
        if (newTask.endDate > milestone.endDate) {
            milestone.endDate = newTask.endDate
        }

        val updatedTask = updateTaskDates(existingTask, newTask.startDate, newTask.endDate, milestone)
        updatedTask.status = newTask.status    // Checks if task status can be updated are done in TasksApiImpl
        updatedTask.name = newTask.name
        updatedTask.lastModifierId = userId
        return taskEntityRepository.persistSuspending(updatedTask)
    }

    suspend fun update(
        existingTask: TaskEntity,
        newStartDate: LocalDate,
        newEndDate: LocalDate,
        milestone: MilestoneEntity,
        userId: UUID,
        proposalModel: Boolean
    ): TaskEntity {
        //if the task extends beyond the milestone, the milestone is updated to fit that task
        if (newStartDate < milestone.startDate) {
            milestone.startDate = newStartDate
        }
        if (newEndDate > milestone.endDate) {
            milestone.endDate = newEndDate
        }

        val updatedTask = updateTaskDates(existingTask, newStartDate, newEndDate, milestone, proposalModel)
        updatedTask.lastModifierId = userId
        return taskEntityRepository.persistSuspending(updatedTask)
    }

    /**
     * Updates task dates
     *
     * @param movableTask task to update
     * @param newTask new task
     * @param milestone milestone
     * @return updated task
     */
    private suspend fun updateTaskDates(
        movableTask: TaskEntity,
        newStartDate: LocalDate,
        newEndDate: LocalDate,
        milestone: MilestoneEntity,
        proposalMode: Boolean = false
    ): TaskEntity {
        if (movableTask.startDate == newStartDate && movableTask.endDate == newEndDate) {
            return movableTask
        }
        val moveForward = movableTask.endDate < newEndDate
        val moveBackward = movableTask.startDate > newStartDate
        movableTask.startDate = newStartDate
        movableTask.endDate = newEndDate

        taskEntityRepository.persistSuspending(movableTask) //Save the task at this point so that when listing connections it is up to date
        Panache.flush()

        val updatableTasks = mutableListOf<TaskEntity>()
        if (moveForward) {
            updateTaskConnectionsForward(movableTask, updatableTasks)
        }
        if (moveBackward) {
            updateTaskConnectionsBackward(movableTask, updatableTasks)
        }

        // Check if the dependent tasks are still within the milestone and save them
        updatableTasks.forEach {
            if (it.startDate < milestone.startDate || it.endDate > milestone.endDate) {
                println("Tasks moved backward are out of milestone, task start date ${it.startDate}, milestone start date ${milestone.startDate}, task end date ${it.endDate}, milestone end date ${milestone.endDate}")
                throw IllegalArgumentException("Tasks moved backward are out of milestone")
            }
        }
        updatableTasks.forEach { taskEntityRepository.persistSuspending(it) }

        //in proposal model cancel all the proposals that affect the moved tasks
        if (proposalMode) {
            proposalController.list(updatableTasks).forEach {
                it.status = ChangeProposalStatus.REJECTED
                proposalController.persist(it)
            }
        }

        return movableTask
    }

    /**
     * Cascade update task connections backward (all parents of movabletask recursively)
     *
     * @param movableTask task to update
     * @param updatableTasks list of tasks to update (this list is updated with new tasks to be saved)
     */
    suspend fun updateTaskConnectionsBackward(movableTask: TaskEntity, updatableTasks: MutableList<TaskEntity>) {
        val tasks = ArrayDeque<TaskEntity>()
        tasks.add(movableTask)
        while (tasks.isNotEmpty()) {
            val currentTask = tasks.removeFirst()
            val connections = taskConnectionController.list(currentTask, TaskConnectionRole.TARGET)
            for (connection in connections) {
                var updated = 0
                val sourceTask = connection.source
                val targetTask = connection.target
                val taskLength = targetTask.endDate.toEpochDay() - targetTask.startDate.toEpochDay()
                when (connection.type) {
                    TaskConnectionType.FINISH_TO_START -> {
                        if (sourceTask.endDate > targetTask.startDate) {
                            updated = 1
                            sourceTask.endDate = targetTask.startDate
                            sourceTask.startDate = sourceTask.endDate.minusDays(taskLength)
                        }
                    }

                    TaskConnectionType.START_TO_START -> {
                        if (sourceTask.startDate > targetTask.startDate) {
                            updated = 1
                            sourceTask.startDate = targetTask.startDate
                            sourceTask.endDate = sourceTask.startDate.plusDays(taskLength)
                        }
                    }

                    TaskConnectionType.FINISH_TO_FINISH -> {
                        if (sourceTask.endDate > targetTask.endDate) {
                            updated = 1
                            sourceTask.endDate = targetTask.endDate
                            sourceTask.startDate = sourceTask.endDate.minusDays(taskLength)
                        }
                    }
                }

                if (updated == 1) {
                    updatableTasks.add(sourceTask)
                }
                tasks.add(sourceTask)
            }

        }
    }

    /**
     * Cascade update task connections forward (all children of movable task recursively)
     *
     * @param movableTask task to update
     * @param updatableTasks list of tasks to update (this list is updated with new tasks to be saved)
     */
    suspend fun updateTaskConnectionsForward(movableTask: TaskEntity, updatableTasks: MutableList<TaskEntity>) {
        val tasks = ArrayDeque<TaskEntity>()
        tasks.add(movableTask)

        while (tasks.isNotEmpty()) {
            val currentTask = tasks.removeFirst()
            val connections = taskConnectionController.list(currentTask, TaskConnectionRole.SOURCE)
            for (connection in connections) {
                var updated = 0
                val sourceTask = connection.source
                val targetTask = connection.target
                val taskLength = targetTask.endDate.toEpochDay() - targetTask.startDate.toEpochDay()
                when (connection.type) {
                    TaskConnectionType.FINISH_TO_START -> {
                        if (sourceTask.endDate > targetTask.startDate) {
                            updated = 1
                            targetTask.startDate = sourceTask.endDate
                            targetTask.endDate = targetTask.startDate.plusDays(taskLength)
                        }
                    }

                    TaskConnectionType.START_TO_START -> {
                        if (targetTask.startDate < sourceTask.startDate) {
                            updated = 1
                            targetTask.startDate = sourceTask.startDate
                            targetTask.endDate = targetTask.startDate.plusDays(taskLength)
                        }
                    }

                    TaskConnectionType.FINISH_TO_FINISH -> {
                        if (targetTask.endDate < sourceTask.endDate) {
                            updated = 1
                            targetTask.endDate = sourceTask.endDate
                            targetTask.startDate = targetTask.endDate.minusDays(taskLength)
                        }
                    }
                }


                if (updated == 1) {
                    updatableTasks.add(targetTask)
                }
                tasks.add(targetTask)
            }
        }
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