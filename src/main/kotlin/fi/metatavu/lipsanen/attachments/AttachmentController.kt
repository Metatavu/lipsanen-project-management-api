package fi.metatavu.lipsanen.attachments

import fi.metatavu.lipsanen.api.model.Attachment
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.tasks.TaskEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for project attachments
 */
@ApplicationScoped
class AttachmentController {

    @Inject
    lateinit var attachmentRepository: AttachmentRepository

    /**
     * Creates a new project attachment
     *
     * @param attachmentRestObject attachment rest object
     * @param project project
     * @param task task
     * @param userId user id
     * @return created project attachment
     */
    suspend fun create(
        attachmentRestObject: Attachment,
        project: ProjectEntity,
        task: TaskEntity?,
        userId: UUID
    ): AttachmentEntity {
        return attachmentRepository.create(
            id = UUID.randomUUID(),
            attachmentUrl = attachmentRestObject.url,
            project = project,
            attachmentType = attachmentRestObject.type,
            attachmentName = attachmentRestObject.name,
            task = task,
            creatorId = userId,
            lastModifierId = userId
        )
    }

    /**
     * Lists attachments
     *
     * @param projects project
     * @param task task
     * @param first first
     * @param max max
     * @return list of attachments
     */
    suspend fun list(
        projects: Array<ProjectEntity>?,
        task: TaskEntity?,
        first: Int?,
        max: Int?
    ): Pair<List<AttachmentEntity>, Long> {
        return attachmentRepository.list(projects, task, first, max)
    }

    /**
     * Lists attachments
     *
     * @param project project
     * @param task task
     * @return list of attachments
     */
    suspend fun list(project: ProjectEntity?, task: TaskEntity?): List<AttachmentEntity> {
        return attachmentRepository.list(project, task)
    }

    /**
     * Finds attachment by id
     *
     * @param attachmentId attachment id
     * @return found attachment or null if not found
     */
    suspend fun find(attachmentId: UUID): AttachmentEntity? {
        return attachmentRepository.findByIdSuspending(attachmentId)
    }

    /**
     * Updates an attachment
     *
     * @param existingAttachment existing attachment
     * @param attachment attachment
     * @param newProject new project
     * @param newTask new task
     * @param loggedInUserId logged in user id
     * @return updated attachment
     */
    suspend fun update(
        existingAttachment: AttachmentEntity,
        attachment: Attachment,
        newProject: ProjectEntity,
        newTask: TaskEntity?,
        loggedInUserId: UUID
    ): AttachmentEntity {
        existingAttachment.url = attachment.url
        existingAttachment.project = newProject
        existingAttachment.attachmentType = attachment.type
        existingAttachment.name = attachment.name
        existingAttachment.task = newTask
        existingAttachment.lastModifierId = loggedInUserId
        return attachmentRepository.persistSuspending(existingAttachment)
    }

    /**
     * Deletes an attachment
     *
     * @param attachment attachment to delete
     */
    suspend fun delete(attachment: AttachmentEntity) {
        return attachmentRepository.deleteSuspending(attachment)
    }

}