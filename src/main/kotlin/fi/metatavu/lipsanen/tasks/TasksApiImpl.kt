package fi.metatavu.lipsanen.tasks

import fi.metatavu.lipsanen.api.model.Task
import fi.metatavu.lipsanen.api.spec.TasksApi
import fi.metatavu.lipsanen.exceptions.TaskOutsideMilestoneException
import fi.metatavu.lipsanen.exceptions.UserNotFoundException
import fi.metatavu.lipsanen.positions.JobPositionController
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import fi.metatavu.lipsanen.tasks.connections.TaskConnectionRepository
import fi.metatavu.lipsanen.proposals.ChangeProposalController
import fi.metatavu.lipsanen.users.UserController
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.core.Vertx
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*

/**
 * Tasks API implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class TasksApiImpl : TasksApi, AbstractApi() {

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var taskTranslator: TaskTranslator

    @Inject
    lateinit var taskConnectionRepository: TaskConnectionRepository

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var jobPositionController: JobPositionController

    @Inject
    lateinit var changeProposalController: ChangeProposalController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listTasks(projectId: UUID?, changeProposalId: UUID?, milestoneId: UUID?, first: Int?, max: Int?): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

            if (changeProposalId != null) {
                val proposal = changeProposalController.find(changeProposalId) ?: return@withCoroutineScope createNotFound(
                    createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
                )
                getProjectAccessRights(proposal.task.milestone.project.id, userId).second?.let { return@withCoroutineScope it }
                return@withCoroutineScope try {
                    val tasks = changeProposalController.listChangeProposalTasksPreview(proposal, userId)
                    createOk(taskTranslator.translate(tasks), tasks.size.toLong())
                } catch (e: TaskOutsideMilestoneException) {
                    createBadRequest(e.message!!)
                }
            }

            val milestoneFilter = if (milestoneId != null) {
                val milestone = milestoneController.find(milestoneId) ?: return@withCoroutineScope createNotFound(
                    createNotFoundMessage(MILESTONE, milestoneId)
                )
                getProjectAccessRights(milestone.project.id, userId).second?.let { return@withCoroutineScope it }
                milestone
            } else null

            val projectFilter = if (projectId != null) {
                val (project, errorResponse) = getProjectAccessRights(projectId, userId)
                if (errorResponse != null) return@withCoroutineScope errorResponse
                arrayOf(project!!)
            } else {
                if (isAdmin()) {
                    null
                } else {
                    val userEntity = userController.findUserByKeycloakId(userId) ?: return@withCoroutineScope createInternalServerError("Failed to find a user")
                    userController.listUserProjects(userEntity).map { it.project }.toTypedArray()
                }
            }

            val (tasks, count) = taskController.list(projectFilter = projectFilter, milestoneFilter = milestoneFilter, first = first, max = max)
            createOk(taskTranslator.translate(tasks), count)
        }

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun createTask(task: Task): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

            if (task.startDate.isAfter(task.endDate)) {
                return@withCoroutineScope createBadRequest(INVALID_TASK_DATES)
            }

            val jobPosition = if (task.jobPositionId != null) {
                jobPositionController.findJobPosition(task.jobPositionId) ?: return@withCoroutineScope createBadRequest(
                    createNotFoundMessage(JOB_POSITION, task.jobPositionId)
                )
            } else null

            val milestone = milestoneController.find(task.milestoneId) ?: return@withCoroutineScope createBadRequest(
                createNotFoundMessage(MILESTONE, task.milestoneId)
            )
            val (project, errorResponse) = getProjectAccessRights(milestone.project, userId)
            if (errorResponse != null) return@withCoroutineScope errorResponse

            if (!isAdmin() && !isProjectOwner() && !projectController.isInPlanningStage(project!!)) {
                return@withCoroutineScope createBadRequest(INVALID_PROJECT_STATE)
            }

            try {
                val createdTask = taskController.create(
                    milestone = milestone,
                    jobPosition = jobPosition,
                    task = task,
                    userId = userId
                )
                createOk(taskTranslator.translate(createdTask))
            } catch (e: UserNotFoundException) {
                Panache.currentTransaction().awaitSuspending().markForRollback()
                createBadRequest(e.message!!)
            }
        }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findTask(taskId: UUID): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

            val task = taskController.find(
                taskId = taskId
            ) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(TASK, taskId))

            getProjectAccessRights(task.milestone.project, userId).second?.let { return@withCoroutineScope it }

            createOk(taskTranslator.translate(task))
        }

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun updateTask(taskId: UUID, task: Task): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

            if (task.startDate.isAfter(task.endDate)) {
                return@withCoroutineScope createBadRequest(INVALID_TASK_DATES)
            }

            val foundTask = taskController.find(taskId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(TASK, taskId)
            )

            val (project, errorResponse) = getProjectAccessRights(foundTask.milestone.project, userId)
            if (errorResponse != null) return@withCoroutineScope errorResponse

            if (!isAdmin() && !isProjectOwner() && !projectController.isInPlanningStage(project!!)) {
                return@withCoroutineScope createBadRequest(INVALID_PROJECT_STATE)
            }

            val jobPosition = if (task.jobPositionId != null) {
                jobPositionController.findJobPosition(task.jobPositionId) ?: return@withCoroutineScope createBadRequest(
                    createNotFoundMessage(JOB_POSITION, task.jobPositionId)
                )
            } else null

            val milestone = milestoneController.find(project!!, task.milestoneId) ?: return@withCoroutineScope createBadRequest(
                createNotFoundMessage(MILESTONE, task.milestoneId)
            )

            // Verify that nothing blocks it from updating
            val updateError = taskController.isNotUpdatable(
                existingTask = foundTask,
                newStatus = task.status
            )
            if (updateError != null) {
                return@withCoroutineScope createConflict(updateError)
            }

            try {
                val updatedTask = taskController.update(
                    existingTask = foundTask,
                    newTask = task,
                    milestone = milestone,
                    jobPosition = jobPosition,
                    userId = userId
                )
                return@withCoroutineScope createOk(taskTranslator.translate(updatedTask))
            } catch (e: TaskOutsideMilestoneException) {
                Panache.currentTransaction().awaitSuspending().markForRollback()
                return@withCoroutineScope createBadRequest(e.message!!)
            } catch (e: UserNotFoundException) {
                Panache.currentTransaction().awaitSuspending().markForRollback()
                return@withCoroutineScope createBadRequest(e.message!!)
            }

        }

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun deleteTask(taskId: UUID): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

            val foundTask = taskController.find(taskId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(TASK, taskId)
            )

            val (project, errorResponse) = getProjectAccessRights(foundTask.milestone.project, userId)
            if (errorResponse != null) return@withCoroutineScope errorResponse

            if (!isAdmin() && !isProjectOwner() && !projectController.isInPlanningStage(project!!)) {
                return@withCoroutineScope createBadRequest(INVALID_PROJECT_STATE)
            }

            if (taskConnectionRepository.countByTask(foundTask) > 0) {
                return@withCoroutineScope createBadRequest("Task has connections and cannot be deleted")
            }

            taskController.delete(foundTask)
            createNoContent()
        }

}