package fi.metatavu.lipsanen.projects.milestones.tasks.comments

import fi.metatavu.lipsanen.api.model.TaskComment
import fi.metatavu.lipsanen.api.spec.TaskCommentsApi
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskController
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

/**
 * Task comments API implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class TaskCommentsApiImpl : TaskCommentsApi, AbstractApi() {

    @Inject
    lateinit var taskCommentController: TaskCommentController

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var taskCommentTranslator: TaskCommentTranslator

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun listTaskComments(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        first: Int?,
        max: Int?
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@async errorResponse
        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskId)
        )

        val (comments, count) = taskCommentController.listTaskComments(task, first, max)
        createOk(taskCommentTranslator.translate(comments), count)
    }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun createTaskComment(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        taskComment: TaskComment
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        if (taskComment.taskId != taskId) return@async createBadRequest("Task id in path and body do not match")
        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@async errorResponse

        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskId)
        )

        taskComment.referencedUsers.forEach { referencedUserId ->
            if (!projectController.hasAccessToProject(task.milestone.project, referencedUserId)) {
                return@async createBadRequest("Referenced user $referencedUserId does not belong to the project")
            }
        }
        val createdComment = taskCommentController.createTaskComment(task, taskComment, userId)
        createOk(taskCommentTranslator.translate(createdComment))
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun findTaskComment(projectId: UUID, milestoneId: UUID, taskId: UUID, commentId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

            val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            if (errorResponse != null) return@async errorResponse

            val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
                createNotFoundMessage(TASK, taskId)
            )

            val comment = taskCommentController.find(commentId)
            if (comment == null || comment.task.id != task.id) return@async createNotFound(
                createNotFoundMessage(TASK_COMMENT, commentId)
            )

            createOk(taskCommentTranslator.translate(comment))
        }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun updateTaskComment(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        commentId: UUID,
        taskComment: TaskComment
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        if (taskComment.taskId != taskId) return@async createBadRequest("Task id in path and body do not match")

        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@async errorResponse

        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskId)
        )

        val comment = taskCommentController.find(commentId) ?: return@async createNotFound(
            createNotFoundMessage(TASK_COMMENT, commentId)
        )

        if (comment.task.id != task.id) return@async createNotFound(
            createNotFoundMessage(TASK_COMMENT, commentId)
        )

        if (comment.creatorId != userId) return@async createForbidden("Cannot edit not own comment")

        taskComment.referencedUsers.forEach { referencedUserId ->
            if (!projectController.hasAccessToProject(task.milestone.project, referencedUserId)) {
                return@async createBadRequest("Referenced user $referencedUserId does not belong to the project")
            }
        }

        val updatedComment = taskCommentController.updateTaskComment(comment, taskComment, userId)
        createOk(taskCommentTranslator.translate(updatedComment))
    }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun deleteTaskComment(projectId: UUID, milestoneId: UUID, taskId: UUID, commentId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

            val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            if (errorResponse != null) return@async errorResponse

            val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
                createNotFoundMessage(TASK, taskId)
            )

            val comment = taskCommentController.find(commentId) ?: return@async createNotFound(
                createNotFoundMessage(TASK_COMMENT, commentId)
            )

            if (comment.task.id != task.id) return@async createNotFound(
                createNotFoundMessage(TASK_COMMENT, commentId)
            )

            if (comment.creatorId != userId) return@async createForbidden("Cannot delete not own comment")

            taskCommentController.deleteTaskComment(comment)
            createNoContent()
        }.asUni()
}