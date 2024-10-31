package fi.metatavu.lipsanen.attachments

import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.tasks.TaskEntity
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for project attachments
 */
@ApplicationScoped
class AttachmentRepository : AbstractRepository<AttachmentEntity, UUID>() {

    /**
     * Creates a new project attachment
     *
     * @param id id
     * @param attachmentUrl attachment url
     * @param project project
     * @param task task
     * @param creatorId creator id
     * @param lastModifierId last modifier id
     * @return created project attachment
     */
    suspend fun create(
        id: UUID,
        attachmentUrl: String,
        project: ProjectEntity,
        attachmentType: String,
        attachmentName: String,
        task: TaskEntity?,
        creatorId: UUID,
        lastModifierId: UUID
    ): AttachmentEntity {
        val projectAttachment = AttachmentEntity()
        projectAttachment.id = id
        projectAttachment.url = attachmentUrl
        projectAttachment.project = project
        projectAttachment.attachmentType = attachmentType
        projectAttachment.name = attachmentName
        projectAttachment.task = task
        projectAttachment.creatorId = creatorId
        projectAttachment.lastModifierId = lastModifierId
        return persistSuspending(projectAttachment)
    }

    /**
     * Lists attachments sorted
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
        val queryBuilder = StringBuilder()
        val parameters = Parameters()

        if (projects != null) {
            addCondition(queryBuilder, "project in :projects")
            parameters.and("projects", projects.toList())
        }

        if (task != null) {
            addCondition(queryBuilder, "task = :task")
            parameters.and("task", task)
        }

        return applyFirstMaxToQuery(
            find(applyOrder(queryBuilder.toString()), parameters),
            first,
            max
        )
    }

    /**
     * Lists attachments
     *
     * @param project project
     * @param task task
     * @return list of attachments
     */
    suspend fun list(project: ProjectEntity?, task: TaskEntity?): List<AttachmentEntity> {
        val queryBuilder = StringBuilder()
        val parameters = Parameters()

        if (project != null) {
            addCondition(queryBuilder, "project = :project")
            parameters.and("project", project)
        }

        if (task != null) {
            addCondition(queryBuilder, "task = :task")
            parameters.and("task", task)
        }

        return find(queryBuilder.toString(), parameters).list<AttachmentEntity>().awaitSuspending()
    }
}