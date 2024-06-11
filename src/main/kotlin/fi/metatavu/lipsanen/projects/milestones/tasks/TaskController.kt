package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.api.model.*
import fi.metatavu.lipsanen.exceptions.TaskOutsideMilestoneException
import fi.metatavu.lipsanen.notifications.NotificationsController
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.projects.milestones.MilestoneEntity
import fi.metatavu.lipsanen.projects.milestones.tasks.comments.TaskCommentController
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

    @Inject
    lateinit var taskAssigneeRepository: TaskAssigneeRepository

    @Inject
    lateinit var taskAttachmentRepository: TaskAttachmentRepository

    @Inject
    lateinit var notificationsController: NotificationsController

    @Inject
    lateinit var taskCommentController: TaskCommentController

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
     * Creates a new task and triggers the needed notifications
     *
     * @param milestone milestone
     * @param task task
     * @param userId user id
     * @return created task
     */
    suspend fun create(milestone: MilestoneEntity, task: Task, userId: UUID): TaskEntity {
        val taskEntity = taskEntityRepository.create(
            id = UUID.randomUUID(),
            name = task.name,
            startDate = task.startDate,
            endDate = task.endDate,
            milestone = milestone,
            status = TaskStatus.NOT_STARTED,
            userRole = task.userRole ?: UserRole.USER,
            estimatedDuration = task.estimatedDuration,
            estimatedReadiness = task.estimatedReadiness,
            creatorId = userId,
            lastModifierId = userId
        )

        task.assigneeIds?.forEach { assigneeId ->
            taskAssigneeRepository.create(
                id = UUID.randomUUID(),
                task = taskEntity,
                assigneeId = assigneeId
            )
        }
        notifyTaskAssignments(taskEntity, task.assigneeIds ?: emptyList(), userId)

        task.attachmentUrls?.forEach { attachmentUrl ->
            taskAttachmentRepository.create(
                id = UUID.randomUUID(),
                task = taskEntity,
                attachmentUrl = attachmentUrl
            )
        }

        return taskEntity
    }

    /**
     * Finds a task
     *
     * @param taskId task id
     * @return found task or null if not found
     */
    suspend fun find(taskId: UUID): TaskEntity? {
        return taskEntityRepository.findByIdSuspending(taskId)
    }

    /**
     * Finds a task within a milestone
     *
     * @param milestone milestone
     * @param taskId task id
     * @return found task or null if not found
     */
    suspend fun find(milestone: MilestoneEntity, taskId: UUID): TaskEntity? {
        return taskEntityRepository.findByIdSuspending(taskId)
            ?.takeIf { it.milestone.id == milestone.id }
    }

    /**
     * Finds a task within a project
     *
     * @param project project
     * @param taskId task id
     * @return found task or null if not found
     */
    suspend fun find(project: ProjectEntity, taskId: UUID): TaskEntity? {
        return taskEntityRepository.findByIdSuspending(taskId)
            ?.takeIf { it.milestone.project.id == project.id }
    }

    /**
     * Updates a task (from REST entity)
     *
     * @param existingTask existing task
     * @param newTask new task
     * @param milestone milestone
     * @param userId user id
     * @return updated task
     * @throws TaskOutsideMilestoneException if the cascade update goes out of the milestone boundaries
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

        updateAssignees(existingTask, newTask.assigneeIds, userId)
        updateAttachments(existingTask, newTask)
        if (existingTask.status != newTask.status) {
            notifyTaskStatusChange(existingTask, newTask.assigneeIds ?: emptyList(), userId)
        }

        val updatedTask = updateTaskDates(existingTask, newTask.startDate, newTask.endDate, milestone)
        updatedTask.status = newTask.status    // Checks if task status can be updated are done in TasksApiImpl
        updatedTask.name = newTask.name
        updatedTask.userRole = newTask.userRole ?: UserRole.USER
        updatedTask.estimatedDuration = newTask.estimatedDuration
        updatedTask.estimatedReadiness = newTask.estimatedReadiness
        updatedTask.lastModifierId = userId


        return taskEntityRepository.persistSuspending(updatedTask)
    }

    /**
     * Updates task attachments
     *
     * @param existingTask existing task
     * @param newTask new task
     */
    suspend fun updateAttachments(
        existingTask: TaskEntity,
        newTask: Task
    ) {
        val existingAttachments = taskAttachmentRepository.listByTask(existingTask)
        val newAttachments = newTask.attachmentUrls ?: emptyList()
        existingAttachments.forEach { existingAttachment ->
            if (existingAttachment.attachmentUrl !in newAttachments) {
                taskAttachmentRepository.deleteSuspending(existingAttachment)
            }
        }
        newAttachments.forEach { newAttachmentUrl ->
            if (existingAttachments.none { it.attachmentUrl == newAttachmentUrl }) {
                taskAttachmentRepository.create(UUID.randomUUID(), existingTask, newAttachmentUrl)
            }
        }
    }

    /**
     * Updates task assignees and sends notifications
     *
     * @param existingTask existing task
     * @param newAssigneeIds new assignee ids
     * @param userId user id
     */
    suspend fun updateAssignees(
        existingTask: TaskEntity,
        newAssigneeIds: List<UUID>?,
        userId: UUID
    ) {
        val existingAssignees = taskAssigneeRepository.listByTask(existingTask)
        val newAssignees = newAssigneeIds ?: emptyList()
        existingAssignees.forEach { existingAssignee ->
            if (existingAssignee.assigneeId !in newAssignees) {
                taskAssigneeRepository.deleteSuspending(existingAssignee)
            }
        }
        newAssignees.forEach { newAssigneeId ->
            if (existingAssignees.none { it.assigneeId == newAssigneeId }) {
                taskAssigneeRepository.create(UUID.randomUUID(), existingTask, newAssigneeId)
                notifyTaskAssignments(existingTask, newAssignees, userId)
            }
        }
    }

    /**
     * Updates a task (from proposal)
     *
     * @param existingTask existing task
     * @param newStartDate new start date
     * @param newEndDate new end date
     * @param milestone milestone
     * @param userId user id
     * @param proposalMode if the task update happens in proposal mode - in this case reject the dependent proposals that affect the tasks affected by the update
     * @return updated task
     * @throws TaskOutsideMilestoneException if the cascade update goes out of the milestone boundaries
     */
    suspend fun applyTaskProposal(
        existingTask: TaskEntity,
        newStartDate: LocalDate,
        newEndDate: LocalDate,
        milestone: MilestoneEntity,
        userId: UUID,
        proposalMode: Boolean
    ): TaskEntity {
        //if the task extends beyond the milestone, the milestone is updated to fit that task
        if (newStartDate < milestone.startDate) {
            milestone.startDate = newStartDate
        }
        if (newEndDate > milestone.endDate) {
            milestone.endDate = newEndDate
        }

        val updatedTask = updateTaskDates(existingTask, newStartDate, newEndDate, milestone, proposalMode)
        updatedTask.lastModifierId = userId
        return taskEntityRepository.persistSuspending(updatedTask)
    }

    /**
     * Deletes a task and related entities
     *
     * @param foundTask task to delete
     */
    suspend fun delete(foundTask: TaskEntity) {
        notificationsController.list(task = foundTask).forEach {
            notificationsController.delete(it)
        }
        taskConnectionController.list(foundTask).forEach {
            taskConnectionController.delete(it)
        }
        proposalController.list(foundTask).forEach {
            proposalController.delete(it)
        }
        taskAssigneeRepository.listByTask(foundTask).forEach {
            taskAssigneeRepository.deleteSuspending(it)
        }
        taskAttachmentRepository.listByTask(foundTask).forEach {
            taskAttachmentRepository.deleteSuspending(it)
        }
        taskCommentController.listTaskComments(foundTask).first.forEach {
            taskCommentController.deleteTaskComment(it)
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

    /**
     * Helper method for checking if task can be updated (used by api)
     *
     * @param existingTask existing task
     * @param newStatus new status
     * @return error message or null if no errors
     */
    suspend fun isNotUpdatable(existingTask: TaskEntity, newStatus: TaskStatus): String? {
        if (existingTask.status != newStatus) {
            val parentTasks = taskConnectionController.list(existingTask, TaskConnectionRole.TARGET)
            for (parentTaskConnection in parentTasks) {
                val source = parentTaskConnection.source
                if (parentTaskConnection.type == TaskConnectionType.FINISH_TO_START &&
                    source.status != TaskStatus.DONE) {
                    return "Task ${source.name} must be finished before task ${existingTask.name} can be started"
                }

                if (parentTaskConnection.type == TaskConnectionType.START_TO_START &&
                    source.status == TaskStatus.NOT_STARTED) {
                    return "Task ${source.name} must be started before task ${existingTask.name} can be started"
                }

                if (parentTaskConnection.type == TaskConnectionType.FINISH_TO_FINISH &&
                    source.status != TaskStatus.DONE) {
                    return "Task ${source.name} must be finished before task ${existingTask.name} can be finished"
                }
            }

        }
        return null
    }

    /**
     * Creates notifications of task assignments
     *
     * @param task task
     * @param receivers task assignees
     * @param userId modifier id
     */
    private suspend fun notifyTaskAssignments(task: TaskEntity, receivers: List<UUID>, userId: UUID) {
        notificationsController.createAndNotify(
            message = "User has been assigned to task ${task.name}",
            type = NotificationType.TASK_ASSIGNED,
            taskEntity = task,
            receiverIds = receivers,
            creatorId = userId
        )
    }

    /**
     * Creates notifications of task status updates
     *
     * @param task task
     * @param receivers task assignees
     * @param userId modifier id
     */
    private suspend fun notifyTaskStatusChange(task: TaskEntity, receivers: List<UUID>, userId: UUID) {
        notificationsController.createAndNotify(
            message = "Status changed to ${task.status}",
            type = NotificationType.TASK_STATUS_CHANGED,
            taskEntity = task,
            receiverIds = receivers,
            creatorId = userId
        )
    }

    /**
     * Updates task dates
     *
     * @param movableTask task to update
     * @param newStartDate new start date
     * @param newEndDate new end date
     * @param milestone milestone
     * @param proposalMode if the task update happens in proposal mode - if the task update happens in proposal mode - in this case reject the dependent proposals that affect the tasks affected by the update
     * @return updated task
     * @throws TaskOutsideMilestoneException if the cascade update goes out of the milestone boundaries
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

        taskEntityRepository.persistSuspending(movableTask) //Save the task at this point so that when listing connections it is up-to-date
        Panache.flush()

        val updatableTasks = mutableListOf<TaskEntity>()
        if (moveForward) {
            updateTaskConnectionsForward(movableTask, updatableTasks)
        }
        if (moveBackward) {
            updateTaskConnectionsBackward(movableTask, updatableTasks)
        }

        // Check if the dependent tasks are still within the milestone
        updatableTasks.forEach {
            if (it.startDate < milestone.startDate || it.endDate > milestone.endDate) {
                throw TaskOutsideMilestoneException(it.id, it.startDate, it.endDate)
            }
        }
        updatableTasks.forEach { taskEntityRepository.persistSuspending(it) }

        //in proposal model cancel all the proposals that affect the moved tasks
        if (proposalMode) {
            proposalController.list(updatableTasks.distinctBy { it.id }).forEach {
                it.status = ChangeProposalStatus.REJECTED
                proposalController.persist(it)
            }
        }

        return movableTask
    }

    /**
     * Cascade update task connections backward (all parents of movedTask recursively)
     *
     * @param movedTask task to update
     * @param updatableTasks list of tasks to update (this list is updated with new tasks to be saved)
     */
    private suspend fun updateTaskConnectionsBackward(movedTask: TaskEntity, updatableTasks: MutableList<TaskEntity>) {
        val tasks = ArrayDeque<TaskEntity>()
        tasks.add(movedTask)
        while (tasks.isNotEmpty()) {
            val currentTask = tasks.removeFirst()
            val connections = taskConnectionController.list(currentTask, TaskConnectionRole.TARGET)
            for (connection in connections) {
                var updated = false
                val sourceTask = connection.source
                val targetTask = connection.target
                val taskLength = targetTask.endDate.toEpochDay() - targetTask.startDate.toEpochDay()
                when (connection.type) {
                    TaskConnectionType.FINISH_TO_START -> {
                        if (sourceTask.endDate > targetTask.startDate) {
                            updated = true
                            sourceTask.endDate = targetTask.startDate
                            sourceTask.startDate = sourceTask.endDate.minusDays(taskLength)
                        }
                    }

                    TaskConnectionType.START_TO_START -> {
                        if (sourceTask.startDate > targetTask.startDate) {
                            updated = true
                            sourceTask.startDate = targetTask.startDate
                            sourceTask.endDate = sourceTask.startDate.plusDays(taskLength)
                        }
                    }

                    TaskConnectionType.FINISH_TO_FINISH -> {
                        if (sourceTask.endDate > targetTask.endDate) {
                            updated = true
                            sourceTask.endDate = targetTask.endDate
                            sourceTask.startDate = sourceTask.endDate.minusDays(taskLength)
                        }
                    }
                }

                if (updated) {
                    updatableTasks.add(sourceTask)
                }
                tasks.add(sourceTask)
            }

        }
    }

    /**
     * Cascade update task connections forward (all children of movedTask recursively)
     *
     * @param movedTask task to update
     * @param updatableTasks list of tasks to update (this list is updated with new tasks to be saved)
     */
    private suspend fun updateTaskConnectionsForward(movedTask: TaskEntity, updatableTasks: MutableList<TaskEntity>) {
        val tasks = ArrayDeque<TaskEntity>()
        tasks.add(movedTask)

        while (tasks.isNotEmpty()) {
            val currentTask = tasks.removeFirst()
            val connections = taskConnectionController.list(currentTask, TaskConnectionRole.SOURCE)
            for (connection in connections) {
                var updated = false
                val sourceTask = connection.source
                val targetTask = connection.target
                val taskLength = targetTask.endDate.toEpochDay() - targetTask.startDate.toEpochDay()
                when (connection.type) {
                    TaskConnectionType.FINISH_TO_START -> {
                        if (sourceTask.endDate > targetTask.startDate) {
                            updated = true
                            targetTask.startDate = sourceTask.endDate
                            targetTask.endDate = targetTask.startDate.plusDays(taskLength)
                        }
                    }

                    TaskConnectionType.START_TO_START -> {
                        if (targetTask.startDate < sourceTask.startDate) {
                            updated = true
                            targetTask.startDate = sourceTask.startDate
                            targetTask.endDate = targetTask.startDate.plusDays(taskLength)
                        }
                    }

                    TaskConnectionType.FINISH_TO_FINISH -> {
                        if (targetTask.endDate < sourceTask.endDate) {
                            updated = true
                            targetTask.endDate = sourceTask.endDate
                            targetTask.startDate = targetTask.endDate.minusDays(taskLength)
                        }
                    }
                }


                if (updated) {
                    updatableTasks.add(targetTask)
                }
                tasks.add(targetTask)
            }
        }
    }

}