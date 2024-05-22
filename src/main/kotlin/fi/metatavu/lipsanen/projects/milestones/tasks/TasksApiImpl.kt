package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.api.model.Task
import fi.metatavu.lipsanen.api.model.TaskConnectionType
import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.api.spec.TasksApi
import fi.metatavu.lipsanen.projects.ProjectController
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.projects.milestones.MilestoneController
import fi.metatavu.lipsanen.projects.milestones.MilestoneEntity
import fi.metatavu.lipsanen.projects.milestones.tasks.connections.TaskConnectionRepository
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*

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
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun listTasks(projectId: UUID, milestoneId: UUID, first: Int?, max: Int?): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

            val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            if (errorResponse != null) return@async errorResponse

            val (tasks, count) = taskController.list(milestone = projectMilestone!!.first, first = first, max = max)
            createOk(taskTranslator.translate(tasks), count)
        }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME)
    override fun createTask(projectId: UUID, milestoneId: UUID, task: Task): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            if (errorResponse != null) return@async errorResponse
            if (task.milestoneId != milestoneId) {
                return@async createBadRequest("Milestone id in task does not match the milestone id in the path")
            }

            if (task.startDate.isAfter(task.endDate)) {
                return@async createBadRequest(INVALID_TASK_DATES)
            }

            if (!projectController.isInPlanningStage(projectMilestone!!.second)) {
                return@async createBadRequest(INVALID_PROJECT_STATE)
            }

            val createdTask = taskController.create(
                milestone = projectMilestone.first,
                task = task,
                userId = userId
            )

            createOk(taskTranslator.translate(createdTask))
        }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun findTask(projectId: UUID, milestoneId: UUID, taskId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            if (errorResponse != null) return@async errorResponse

            val task = taskController.find(
                milestone = projectMilestone!!.first,
                taskId = taskId
            ) ?: return@async createNotFound(createNotFoundMessage(TASK, taskId))

            createOk(taskTranslator.translate(task))
        }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun updateTask(projectId: UUID, milestoneId: UUID, taskId: UUID, task: Task): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            if (errorResponse != null) return@async errorResponse
            if (task.milestoneId != milestoneId) {
                return@async createBadRequest("Milestone id in task does not match the milestone id in the path")
            }
            if (task.startDate.isAfter(task.endDate)) {
                return@async createBadRequest(INVALID_TASK_DATES)
            }

            val foundTask = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
                createNotFoundMessage(TASK, taskId)
            )

            if (!projectController.isInPlanningStage(projectMilestone.second)) {
                return@async createBadRequest(INVALID_PROJECT_STATE)
            }

            // Verify that nothing blocks it from updating
            val updateError = isNotUpdatable(
                existingTask = foundTask,
                newStatus = task.status
            )
            if (updateError != null) {
                return@async createConflict(updateError)
            }

            val updatedTask = taskController.update(
                existingTask = foundTask,
                newTask = task,
                milestone = projectMilestone.first,
                userId = userId
            )

            return@async createOk(taskTranslator.translate(updatedTask))
        }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME)
    override fun deleteTask(projectId: UUID, milestoneId: UUID, taskId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            if (errorResponse != null) return@async errorResponse
            if (!projectController.isInPlanningStage(projectMilestone!!.second)) {
                return@async createBadRequest(INVALID_PROJECT_STATE)
            }

            val foundTask = taskController.find(projectMilestone.first, taskId) ?: return@async createNotFound(
                createNotFoundMessage(TASK, taskId)
            )

            if (taskConnectionRepository.countByTask(foundTask) > 0) {
                return@async createBadRequest("Task has connections and cannot be deleted")
            }

            taskController.delete(foundTask)
            createNoContent()
        }.asUni()



    /**
     * Helper method for checking if task can be updated
     *
     * @param existingTask existing task
     * @param newStatus new status
     * @return error message or null if no errors
     */
    suspend fun isNotUpdatable(existingTask: TaskEntity, newStatus: TaskStatus): String? {
        if (existingTask.status != newStatus) {
            val parentTasks = taskConnectionRepository.listByTargetTask(existingTask)
            for (parentTaskConnection in parentTasks) {
                val source = parentTaskConnection.source
                if (parentTaskConnection.type == TaskConnectionType.FINISH_TO_START) {
                    if (source.status != TaskStatus.DONE) {
                        return "Task ${source.name} must be finished before task ${existingTask.name} can be started"
                    }
                }

                if (parentTaskConnection.type == TaskConnectionType.START_TO_START) {
                    if (source.status == TaskStatus.NOT_STARTED) {
                        return "Task ${source.name} must be started before task ${existingTask.name} can be started"
                    }
                }

                if (parentTaskConnection.type == TaskConnectionType.FINISH_TO_FINISH) {
                    if (source.status != TaskStatus.DONE) {
                        return "Task ${source.name} must be finished before task ${existingTask.name} can be finished"
                    }
                }
            }

        }
        return null
    }
}