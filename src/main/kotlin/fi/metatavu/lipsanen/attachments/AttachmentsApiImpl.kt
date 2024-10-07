package fi.metatavu.lipsanen.attachments

import fi.metatavu.lipsanen.api.model.Attachment
import fi.metatavu.lipsanen.api.spec.AttachmentsApi
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import fi.metatavu.lipsanen.tasks.TaskController
import fi.metatavu.lipsanen.users.UserController
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import java.util.*

@RequestScoped
@WithSession
class AttachmentsApiImpl : AttachmentsApi, AbstractApi() {

    @Inject
    lateinit var attachmentController: AttachmentController

    @Inject
    lateinit var attachmentTranslator: AttachmentTranslator

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var userController: UserController

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listAttachments(projectId: UUID?, taskId: UUID?, first: Int?, max: Int?): Uni<Response> =
        withCoroutineScope {
            val loggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

            val projectFilter = if (projectId != null) {
                val (project, errorResponse) = getProjectAccessRights(projectId, loggedInUserId)
                if (errorResponse != null) return@withCoroutineScope errorResponse
                arrayOf(project!!)
            } else {
                if (isAdmin()) {
                    null
                } else {
                    val userEntity = userController.findUser(loggedInUserId)
                        ?: return@withCoroutineScope createInternalServerError(
                            createNotFoundMessage(
                                USER,
                                loggedInUserId
                            )
                        )
                    userController.listUserProjects(userEntity).map { it.project }.toTypedArray()
                }
            }

            val taskFilter = if (taskId != null) {
                val found = taskController.find(taskId) ?: return@withCoroutineScope createNotFound(
                    createNotFoundMessage(
                        TASK, taskId
                    )
                )
                getProjectAccessRights(
                    found.milestone.project,
                    loggedInUserId
                ).second?.let { return@withCoroutineScope it }
                found
            } else null

            val (attachments, count) = attachmentController.list(
                projects = projectFilter,
                task = taskFilter,
                first = first,
                max = max
            )
            createOk(attachmentTranslator.translate(attachments), count)
        }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun createAttachment(attachment: Attachment): Uni<Response> = withCoroutineScope {
        val loggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val project = projectController.findProject(attachment.projectId)
            ?: return@withCoroutineScope createBadRequest("Project not found")
        getProjectAccessRights(project, loggedInUserId).second?.let { return@withCoroutineScope it }

        val task = attachment.taskId?.let {
            taskController.find(project, it) ?: return@withCoroutineScope createBadRequest(
                createNotFoundMessage(
                    TASK, attachment.taskId
                )
            )
        }
        val createdAttachment = attachmentController.create(attachment, project, task, loggedInUserId)
        createOk(attachmentTranslator.translate(createdAttachment))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findAttachment(attachmentId: UUID): Uni<Response> = withCoroutineScope {
        val loggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val attachment = attachmentController.find(attachmentId)
            ?: return@withCoroutineScope createNotFound(createNotFoundMessage(ATTACHMENT, attachmentId))

        getProjectAccessRights(attachment.project, loggedInUserId).second?.let { return@withCoroutineScope it }
        createOk(attachmentTranslator.translate(attachment))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun updateAttachment(attachmentId: UUID, attachment: Attachment): Uni<Response> = withCoroutineScope {
        val loggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val existingAttachment = attachmentController.find(attachmentId)
            ?: return@withCoroutineScope createBadRequest(createNotFoundMessage(ATTACHMENT, attachmentId))
        getProjectAccessRights(existingAttachment.project, loggedInUserId).second?.let { return@withCoroutineScope it }


        val newProject = projectController.findProject(attachment.projectId)
            ?: return@withCoroutineScope createBadRequest(createNotFoundMessage(PROJECT, attachment.projectId))
        getProjectAccessRights(newProject, loggedInUserId).second?.let { return@withCoroutineScope it }
        val newTask = attachment.taskId?.let {
            taskController.find(newProject, it) ?: return@withCoroutineScope createBadRequest(
                createNotFoundMessage(
                    TASK, attachment.projectId
                )
            )
        }

        val updatedAttachment = attachmentController.update(
            existingAttachment,
            attachment,
            newProject,
            newTask,
            loggedInUserId
        )

        createOk(attachmentTranslator.translate(updatedAttachment))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun deleteAttachment(attachmentId: UUID): Uni<Response> = withCoroutineScope {
        val loggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val attachment = attachmentController.find(attachmentId)
            ?: return@withCoroutineScope createNotFound(createNotFoundMessage(ATTACHMENT, attachmentId))

        getProjectAccessRights(attachment.project, loggedInUserId).second?.let { return@withCoroutineScope it }

        attachmentController.delete(attachment)
        createNoContent()
    }
}