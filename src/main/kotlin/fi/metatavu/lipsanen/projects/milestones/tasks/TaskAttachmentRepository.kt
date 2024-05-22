package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.persistence.AbstractRepository
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Task attachment repository
 */
@ApplicationScoped
class TaskAttachmentRepository : AbstractRepository<TaskAttachmentEntity, UUID>() {
    /**
     * Creates a new task attachment
     *
     * @param id id
     * @param task task
     * @param attachmentUrl attachment url
     * @return created task attachment
     */
    suspend fun create(
        id: UUID,
        task: TaskEntity,
        attachmentUrl: String
    ): TaskAttachmentEntity {
        val taskAttachment = TaskAttachmentEntity()
        taskAttachment.id = id
        taskAttachment.task = task
        taskAttachment.attachmentUrl = attachmentUrl
        return taskAttachment
    }

    /**
     * Lists task attachments by task
     *
     * @param task task
     * @return list of task attachments
     */
    suspend fun listByTask(task: TaskEntity): Pair<List<TaskAttachmentEntity>, Long> {
        val queryBuilder = StringBuilder()
        val params = Parameters()

        queryBuilder.append("task = :task")
        params.and("task", task)

        return applyFirstMaxToQuery(
            query = find(queryBuilder.toString(), Sort.descending("id"), params),
            firstIndex = null,
            maxResults = null
        )
    }
}