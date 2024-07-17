package fi.metatavu.lipsanen.projects.milestones.tasks.comments

import fi.metatavu.lipsanen.api.model.TaskComment
import fi.metatavu.lipsanen.api.spec.TaskCommentsApi
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskController
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import fi.metatavu.lipsanen.users.UserController
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.vertx.core.Vertx
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    lateinit var userController: UserController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listTaskComments(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        first: Int?,
        max: Int?
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@withCoroutineScope errorResponse
        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(TASK, taskId)
        )

        val (comments, count) = taskCommentController.listTaskComments(task, first, max)
        createOk(taskCommentTranslator.translate(comments), count)
    }

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun createTaskComment(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        taskComment: TaskComment
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        if (taskComment.taskId != taskId) return@withCoroutineScope createBadRequest("Task id in path and body do not match")
        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@withCoroutineScope errorResponse

        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(TASK, taskId)
        )

        taskComment.referencedUsers.forEach { referencedUserId ->
            val user = userController.findUser(referencedUserId) ?: return@withCoroutineScope createBadRequest(
                "Referenced user $referencedUserId does not exist"
            )
            if (!projectController.hasAccessToProject(task.milestone.project, user.keycloakId)) {
                return@withCoroutineScope createBadRequest("Referenced user $referencedUserId has no access to the project")
            }
        }
        val createdComment = taskCommentController.createTaskComment(task, taskComment, userId)
        createOk(taskCommentTranslator.translate(createdComment))
    }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findTaskComment(projectId: UUID, milestoneId: UUID, taskId: UUID, commentId: UUID): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

            val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            if (errorResponse != null) return@withCoroutineScope errorResponse

            val task = taskController.find(projectMilestone!!.first, taskId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(TASK, taskId)
            )

            val comment = taskCommentController.find(commentId)
            if (comment == null || comment.task.id != task.id) return@withCoroutineScope createNotFound(
                createNotFoundMessage(TASK_COMMENT, commentId)
            )

            createOk(taskCommentTranslator.translate(comment))
        }

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun updateTaskComment(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        commentId: UUID,
        taskComment: TaskComment
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        if (taskComment.taskId != taskId) return@withCoroutineScope createBadRequest("Task id in path and body do not match")

        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@withCoroutineScope errorResponse

        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(TASK, taskId)
        )

        val comment = taskCommentController.find(commentId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(TASK_COMMENT, commentId)
        )

        if (comment.task.id != task.id) return@withCoroutineScope createNotFound(
            createNotFoundMessage(TASK_COMMENT, commentId)
        )

        if (!isAdmin() && !isProjectOwner() && comment.creatorId != userId) return@withCoroutineScope createForbidden("Cannot edit not own comment")

        taskComment.referencedUsers.forEach { referencedUserId ->
            val user = userController.findUser(referencedUserId) ?: return@withCoroutineScope createBadRequest(
                "Referenced user $referencedUserId does not exist"
            )
            if (!projectController.hasAccessToProject(task.milestone.project, user.keycloakId)) {
                return@withCoroutineScope createBadRequest("Referenced user $referencedUserId is not an assignee of the task")
            }
        }

        val updatedComment = taskCommentController.updateTaskComment(comment, taskComment, userId)
        createOk(taskCommentTranslator.translate(updatedComment))
    }

    @WithTransaction
    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun deleteTaskComment(projectId: UUID, milestoneId: UUID, taskId: UUID, commentId: UUID): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

            val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            if (errorResponse != null) return@withCoroutineScope errorResponse

            val task = taskController.find(projectMilestone!!.first, taskId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(TASK, taskId)
            )

            val comment = taskCommentController.find(commentId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(TASK_COMMENT, commentId)
            )

            if (comment.task.id != task.id) return@withCoroutineScope createNotFound(
                createNotFoundMessage(TASK_COMMENT, commentId)
            )

            if (!isAdmin() && !isProjectOwner() && comment.creatorId != userId) return@withCoroutineScope createForbidden("Cannot delete not own comment")

            taskCommentController.deleteTaskComment(comment)
            createNoContent()
        }
}