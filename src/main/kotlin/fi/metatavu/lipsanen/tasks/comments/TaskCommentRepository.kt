package fi.metatavu.lipsanen.tasks.comments

import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.tasks.TaskEntity
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Task comment repository
 */
@ApplicationScoped
class TaskCommentRepository : AbstractRepository<fi.metatavu.lipsanen.tasks.comments.TaskCommentEntity, UUID>() {

    /**
     * Creates task comment
     *
     * @param id id
     * @param task task
     * @param comment comment
     * @param creatorId creator id
     * @return created task comment
     */
    suspend fun create(id: UUID, task: TaskEntity, comment: String, creatorId: UUID): fi.metatavu.lipsanen.tasks.comments.TaskCommentEntity {
        val taskComment = fi.metatavu.lipsanen.tasks.comments.TaskCommentEntity()
        taskComment.id = id
        taskComment.task = task
        taskComment.comment = comment
        taskComment.creatorId = creatorId
        taskComment.lastModifierId = creatorId
        return persistSuspending(taskComment)
    }

}