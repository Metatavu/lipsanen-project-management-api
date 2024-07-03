package fi.metatavu.lipsanen.projects.milestones.tasks.connections

import fi.metatavu.lipsanen.api.model.TaskConnection
import fi.metatavu.lipsanen.api.model.TaskConnectionRole
import fi.metatavu.lipsanen.api.spec.TaskConnectionsApi
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskController
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*

/**
 * Task connections API implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class TaskConnectionsApiImpl : TaskConnectionsApi, AbstractApi() {

    @Inject
    lateinit var taskConnectionTranslator: TaskConnectionTranslator

    @Inject
    lateinit var taskConnectionController: TaskConnectionController

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var vertx: io.vertx.core.Vertx

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listTaskConnections(
        projectId: UUID,
        taskId: UUID?,
        connectionRole: TaskConnectionRole?
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val (project, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@async errorResponse

        val task = if (taskId != null) {
            taskController.find(project!!, taskId) ?: return@async createNotFound(createNotFoundMessage(TASK, taskId))
        } else null

        val taskConnections = taskConnectionController.list(
            project = project!!,
            task = task,
            connectionRole = connectionRole
        )

        createOk(taskConnectionTranslator.translate(taskConnections))
    }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun createTaskConnection(
        projectId: UUID,
        taskConnection: TaskConnection
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val (project, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@async errorResponse

        val sourceTask = taskController.find(project!!, taskConnection.sourceTaskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskConnection.sourceTaskId)
        )
        val targetTask = taskController.find(project, taskConnection.targetTaskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskConnection.targetTaskId)
        )

        taskConnectionController.verifyTaskConnection(sourceTask, targetTask, taskConnection.type)?.let {
            return@async createBadRequest(it)
        }

        val createdTaskConnection = taskConnectionController.create(sourceTask, targetTask, taskConnection, userId)
        createOk(taskConnectionTranslator.translate(createdTaskConnection))
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findTaskConnection(
        projectId: UUID,
        connectionId: UUID
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val (project, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@async errorResponse

        val taskConnection = taskConnectionController.findById(connectionId, project!!) ?: return@async createNotFound(
            createNotFoundMessage(TASK_CONNECTION, connectionId)
        )

        createOk(taskConnectionTranslator.translate(taskConnection))
    }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun updateTaskConnection(
        projectId: UUID,
        connectionId: UUID,
        taskConnection: TaskConnection
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val (project, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@async errorResponse

        val connectionFrom = taskController.find(project!!, taskConnection.sourceTaskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskConnection.sourceTaskId)
        )
        val connectionTo = taskController.find(project, taskConnection.targetTaskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskConnection.targetTaskId)
        )

        taskConnectionController.verifyTaskConnection(connectionFrom, connectionTo, taskConnection.type)?.let {
            return@async createBadRequest(it)
        }

        val foundConnection = taskConnectionController.findById(connectionId, project) ?: return@async createNotFound(
            createNotFoundMessage(TASK_CONNECTION, connectionId)
        )
        val updatedConnection = taskConnectionController.update(
            foundConnection = foundConnection,
            sourceTask = connectionFrom,
            targetTask = connectionTo,
            updatedTaskConnection = taskConnection
        )
        createOk(taskConnectionTranslator.translate(updatedConnection))
    }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun deleteTaskConnection(
        projectId: UUID,
        connectionId: UUID
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val (project, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@async errorResponse

        val foundConnection = taskConnectionController.findById(connectionId, project!!) ?: return@async createNotFound(
            createNotFoundMessage(TASK_CONNECTION, connectionId)
        )
        taskConnectionController.delete(foundConnection)

        createNoContent()
    }.asUni()
}