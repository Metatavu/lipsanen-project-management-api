package fi.metatavu.lipsanen.tasks

import fi.metatavu.lipsanen.api.model.*
import fi.metatavu.lipsanen.attachments.AttachmentController
import fi.metatavu.lipsanen.exceptions.TaskOutsideMilestoneException
import fi.metatavu.lipsanen.exceptions.UserNotFoundException
import fi.metatavu.lipsanen.milestones.MilestoneEntity
import fi.metatavu.lipsanen.milestones.MilestoneRepository
import fi.metatavu.lipsanen.notifications.NotificationsController
import fi.metatavu.lipsanen.positions.JobPositionEntity
import fi.metatavu.lipsanen.projects.ProjectController
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.proposals.ChangeProposalController
import fi.metatavu.lipsanen.tasks.comments.TaskCommentController
import fi.metatavu.lipsanen.tasks.connections.TaskConnectionController
import fi.metatavu.lipsanen.users.UserController
import fi.metatavu.lipsanen.users.UserEntity
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
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
    lateinit var notificationsController: NotificationsController

    @Inject
    lateinit var taskCommentController: TaskCommentController

    @Inject
    lateinit var projectController: ProjectController

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var milestoneRepository: MilestoneRepository

    @Inject
    lateinit var attachmentController: AttachmentController

    /**
     * Lists tasks
     *
     * @param projectFilter project filter
     * @param milestoneFilter milestone
     * @param first first result
     * @param max max results
     * @return list of tasks
     */
    suspend fun list(
        projectFilter: Array<ProjectEntity>?,
        milestoneFilter: MilestoneEntity?,
        first: Int?,
        max: Int?
    ): Pair<List<TaskEntity>, Long> {
        val query = StringBuilder()
        val parameters = Parameters()

        if (projectFilter != null) {
            query.append("milestone.project in :projects")
            parameters.and("projects", projectFilter.toList())
        }

        if (milestoneFilter != null) {
            query.append(if (query.isNotEmpty()) " and " else " ").append("milestone = :milestone")
            parameters.and("milestone", milestoneFilter)
        }

        return taskEntityRepository.applyFirstMaxToQuery(
            query = taskEntityRepository.find(query.toString(), Sort.ascending("startDate"), parameters),
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
     * Lists tasks by milestones
     *
     * @param milestones milestones
     * @return list of tasks
     */
    suspend fun list(milestones: List<MilestoneEntity>): List<TaskEntity> {
        return taskEntityRepository.find("milestone in :milestones", Parameters().and("milestones", milestones))
            .list<TaskEntity>().awaitSuspending()
    }

    /**
     * Lists tasks by dependent user
     *
     * @param dependentUser dependent user
     * @return list of tasks
     */
    suspend fun list(dependentUser: UserEntity): List<TaskEntity> {
        return taskEntityRepository.find("dependentUser", dependentUser).list<TaskEntity>().awaitSuspending()
    }

    /**
     * Creates a new task and triggers the needed notifications, assigns users to the project if not yet done
     *
     * @param milestone milestone
     * @param task task
     * @param userId user id
     * @return created task
     * @throws UserNotFoundException if assignee or dependent user is not found
     */
    suspend fun create(
        milestone: MilestoneEntity,
        jobPosition: JobPositionEntity?,
        task: Task,
        userId: UUID
    ): TaskEntity {
        val dependentUser = task.dependentUserId?.let { getVerifyUserIsInProject(milestone.project, it) }

        val taskEntity = taskEntityRepository.create(
            id = UUID.randomUUID(),
            name = task.name,
            startDate = task.startDate,
            endDate = task.endDate,
            milestone = milestone,
            jobPosition = jobPosition,
            dependentUser = dependentUser,
            userRole = task.userRole,
            status = TaskStatus.NOT_STARTED,
            estimatedDuration = task.estimatedDuration,
            estimatedReadiness = task.estimatedReadiness,
            creatorId = userId,
            lastModifierId = userId
        )

        val assignees = task.assigneeIds?.map { assigneeId ->
            val user = getVerifyUserIsInProject(milestone.project, assigneeId)
            taskAssigneeRepository.create(
                id = UUID.randomUUID(),
                task = taskEntity,
                user = user
            )
            user
        } ?: emptyList()
        notifyTaskAssignments(taskEntity, assignees, userId)
        extendMilestoneToTask(taskEntity.startDate, taskEntity.endDate, milestone)
        updateMilestoneReadiness(milestone)
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
     * @param milestone milestone (cannot be updated but its data is needed)
     * @param jobPosition position
     * @param userId user id
     *
     * @return updated task
     * @throws TaskOutsideMilestoneException if the cascade update goes out of the milestone boundaries
     * @throws UserNotFoundException if assignee or dependent user is not found
     */
    suspend fun update(
        existingTask: TaskEntity,
        newTask: Task,
        milestone: MilestoneEntity,
        jobPosition: JobPositionEntity?,
        userId: UUID
    ): TaskEntity {
        //if the task extends beyond the milestone, the milestone is updated to fit that task
        if (newTask.startDate < milestone.startDate) {
            milestone.startDate = newTask.startDate
        }
        if (newTask.endDate > milestone.endDate) {
            milestone.endDate = newTask.endDate
        }

        val statusUpdated = if (existingTask.status != newTask.status) true else false
        val dependentUser = newTask.dependentUserId?.let { getVerifyUserIsInProject(milestone.project, it) }
        val updatedTasks = getUpdatedTaskDates(existingTask, newTask.startDate, newTask.endDate, milestone)
        val mainUpdatedTask = updatedTasks.find { it.id == existingTask.id } ?: existingTask
        mainUpdatedTask.status = newTask.status    // Checks if task status can be updated are done in TasksApiImpl
        mainUpdatedTask.name = newTask.name
        mainUpdatedTask.userRole = newTask.userRole ?: UserRole.USER
        mainUpdatedTask.estimatedDuration = newTask.estimatedDuration
        mainUpdatedTask.estimatedReadiness = if (newTask.status == TaskStatus.DONE) 100 else newTask.estimatedReadiness
        mainUpdatedTask.jobPosition = jobPosition
        mainUpdatedTask.dependentUser = dependentUser
        mainUpdatedTask.lastModifierId = userId

        extendMilestoneToTask(mainUpdatedTask.startDate, mainUpdatedTask.endDate, milestone)
        val updated = taskEntityRepository.persistSuspending(mainUpdatedTask)

        // Send notifications
        updateAssignees(mainUpdatedTask, newTask.assigneeIds, userId)
        if (statusUpdated) {
            notifyTaskStatusChange(mainUpdatedTask, newTask.status, taskAssigneeRepository.listByTask(existingTask).map { it.user }, userId)
        }

        // Update milestone readiness after task update
        updateMilestoneReadiness(milestone)
        return updated
    }

    /**
     * Updates task assignees and sends notifications
     *
     * @param existingTask existing task
     * @param newAssigneeIds new assignee ids
     * @param userId user id
     * @throws UserNotFoundException if assignee is not found
     */
    suspend fun updateAssignees(
        existingTask: TaskEntity,
        newAssigneeIds: List<UUID>?,
        userId: UUID
    ) {
        val assignedUsers = taskAssigneeRepository.listByTask(existingTask)
        val assignedUserIds = assignedUsers.map { it.user.id }
        val newAssignees = newAssigneeIds ?: emptyList()

        val newlyAssignedUsers = mutableListOf<UserEntity>()

        assignedUsers.forEach { existingAssignee ->
            if (existingAssignee.user.id !in newAssignees) {
                taskAssigneeRepository.deleteSuspending(existingAssignee)
            }
        }
        newAssignees.forEach { newAssigneeId ->
            if (assignedUserIds.none { it == newAssigneeId }) {
                val user = userController.findUser(newAssigneeId) ?: throw UserNotFoundException(newAssigneeId)
                if (!projectController.hasAccessToProject(existingTask.milestone.project, user.id)) {
                    userController.assignUserToProjects(user, listOf(existingTask.milestone.project))
                }
                taskAssigneeRepository.create(UUID.randomUUID(), existingTask, user)
                newlyAssignedUsers.add(user)
            }
        }

        if (newlyAssignedUsers.isNotEmpty()) {
            notifyTaskAssignments(existingTask, newlyAssignedUsers, userId)
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
     * @return updated task
     * @throws TaskOutsideMilestoneException if the cascade update goes out of the milestone boundaries
     */
    suspend fun getTasksAffectedByChangeProposal(
        existingTask: TaskEntity,
        newStartDate: LocalDate,
        newEndDate: LocalDate,
        milestone: MilestoneEntity,
        userId: UUID,
    ): List<TaskEntity> {
        //if the task extends beyond the milestone, the milestone is updated to fit that task
        if (newStartDate < milestone.startDate) {
            milestone.startDate = newStartDate
        }
        if (newEndDate > milestone.endDate) {
            milestone.endDate = newEndDate
        }
        existingTask.lastModifierId = userId
        val updatedTasks = getUpdatedTaskDates(existingTask, newStartDate, newEndDate, milestone)
        return updatedTasks
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
     * Extends the milestone if needed to fit the task in
     *
     * @param taskStartDate task start date
     * @param taskEndDate task end date
     * @param milestone milestone
     */
    private suspend fun extendMilestoneToTask(taskStartDate: LocalDate, taskEndDate: LocalDate, milestone: MilestoneEntity) {
        if (taskStartDate < milestone.startDate) {
            milestone.startDate = taskStartDate
        }
        if (taskEndDate > milestone.endDate) {
            milestone.endDate = taskEndDate
        }
        milestoneRepository.persistSuspending(milestone)
    }

    /**
     * Updates milestone readiness after task update
     *
     * @param milestone milestone
     */
    private suspend fun updateMilestoneReadiness(milestone: MilestoneEntity) {
        val tasks = list(milestone)
        val totalReadiness = tasks.sumOf { it.estimatedReadiness ?: 0 }
        val tasksCount = tasks.size
        val readiness = if (tasksCount > 0 ) {
            totalReadiness / tasksCount
        } else 0
        milestone.estimatedReadiness = readiness
        milestoneRepository.persistSuspending(milestone)
    }

    /**
     * Creates notifications of task assignments
     *
     * @param task task
     * @param receivers task assignees
     * @param userId modifier id
     */
    private suspend fun notifyTaskAssignments(task: TaskEntity, receivers: List<UserEntity>, userId: UUID) {
        notificationsController.createAndNotify(
            type = NotificationType.TASK_ASSIGNED,
            taskEntity = task,
            receivers = receivers,
            creatorId = userId
        )
    }

    /**
     * Creates notifications of task status updates
     *
     * @param task task
     * @param assignees task assignees
     * @param userId modifier id
     */
    private suspend fun notifyTaskStatusChange(
        task: TaskEntity,
        newTaskStatus: TaskStatus,
        assignees: List<UserEntity>,
        userId: UUID
    ) {
        notificationsController.createAndNotify(
            type = NotificationType.TASK_STATUS_CHANGED,
            taskEntity = task,
            newTaskStatus = newTaskStatus,
            receivers = assignees,
            creatorId = userId
        )
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
        attachmentController.list(project = null, task = foundTask).forEach {
            attachmentController.delete(it)
        }
        taskCommentController.listTaskComments(foundTask).first.forEach {
            taskCommentController.deleteTaskComment(it)
        }
        taskEntityRepository.deleteSuspending(foundTask)
        updateMilestoneReadiness(foundTask.milestone)
    }

    /**
     * Updates task dates
     *
     * @param movableTask task to update
     * @param newStartDate new start date
     * @param newEndDate new end date
     * @param milestone milestone
     * @return updated task
     * @throws TaskOutsideMilestoneException if the cascade update goes out of the milestone boundaries
     */
    private suspend fun getUpdatedTaskDates(
        movableTask: TaskEntity,
        newStartDate: LocalDate,
        newEndDate: LocalDate,
        milestone: MilestoneEntity,
    ): List<TaskEntity> {
        if (movableTask.startDate == newStartDate && movableTask.endDate == newEndDate) {
            return listOf(movableTask)
        }

        val moveForward = movableTask.endDate < newEndDate
        val moveBackward = movableTask.startDate > newStartDate
        movableTask.startDate = newStartDate
        movableTask.endDate = newEndDate

        val updatableTasks = mutableListOf(movableTask)
        if (moveForward) {
            updateTaskConnectionsForward(movableTask, updatableTasks)
        }
        if (moveBackward) {
            updateTaskConnectionsBackward(movableTask, updatableTasks)
        }

        // Check if the tasks are still within the milestone (milestone already has modified dates if
        // the original task moved it)
        updatableTasks.forEach {
            if (it.startDate < milestone.startDate || it.endDate > milestone.endDate) {
                throw TaskOutsideMilestoneException(it.id, it.startDate, it.endDate)
            }
        }
        return updatableTasks
    }

    /**
     * Cascade update task connections backward (all parents of movedTask recursively)
     *
     * @param movableTask task to update (need to have up-to-date dates)
     * @param updatableTasks list of tasks to update (this list is updated with new tasks to be saved)
     */
    private suspend fun updateTaskConnectionsBackward(movableTask: TaskEntity, updatableTasks: MutableList<TaskEntity>) {
        val tasks = ArrayDeque<TaskEntity>()
        tasks.add(movableTask)

        while (tasks.isNotEmpty()) {
            val currentTask = tasks.removeFirst()
            val connections = taskConnectionController.list(currentTask, TaskConnectionRole.TARGET)
            for (connection in connections) {
                var updated = false
                val sourceTask = getUpdatedTaskIfAny(connection.source, updatableTasks)
                val targetTask = getUpdatedTaskIfAny(connection.target, updatableTasks)
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
                    updatableTasks.removeIf { it.id == sourceTask.id }
                    updatableTasks.add(sourceTask)
                }
                tasks.add(sourceTask)
            }

        }
    }

    /**
     * Cascade update task connections forward (all children of movedTask recursively)
     *
     * @param movableTask task to update (need to have up-to-date dates)
     * @param updatableTasks list of tasks to update (this list is updated with new tasks to be saved)
     */
    private suspend fun updateTaskConnectionsForward(movableTask: TaskEntity, updatableTasks: MutableList<TaskEntity>) {
        val tasks = ArrayDeque<TaskEntity>()
        tasks.add(movableTask)

        while (tasks.isNotEmpty()) {
            val currentTask = tasks.removeFirst()
            val connections = taskConnectionController.list(currentTask, TaskConnectionRole.SOURCE)
            for (connection in connections) {
                var updated = false
                val sourceTask = getUpdatedTaskIfAny(connection.source, updatableTasks)
                val targetTask = getUpdatedTaskIfAny(connection.target, updatableTasks)
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
                    updatableTasks.removeIf { it.id == targetTask.id }
                    updatableTasks.add(targetTask)
                }
                tasks.add(targetTask)
            }
        }
    }

    /**
     * Helper method for getting updated task from the list
     * It is needed for updateTaskConnectionsForward and updateTaskConnectionsBackwards methods in order to
     * use the task with up-to-date data if available
     *
     * @param entity task that is needed, can be used if it is not in updated task list
     * @param allUpdatedTasks all tasks with changes which are not yet in db
     * @return task
     */
    private fun getUpdatedTaskIfAny(entity: TaskEntity, allUpdatedTasks: List<TaskEntity>): TaskEntity {
        return allUpdatedTasks.find { it.id == entity.id } ?: entity
    }

    private suspend fun getVerifyUserIsInProject(project: ProjectEntity, userId: UUID): UserEntity {
        val user = userController.findUser(userId)
            ?: throw UserNotFoundException(userId)

        if (!projectController.hasAccessToProject(project, user.id)) {
            logger.info("Assigning user ${user.id} to project ${project.id} because of the task assignment")
            userController.assignUserToProjects(
                user = user,
                newProjects = listOf(project)
            )
        }

        return user
    }

}