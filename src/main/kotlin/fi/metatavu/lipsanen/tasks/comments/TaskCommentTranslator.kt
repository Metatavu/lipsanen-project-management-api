package fi.metatavu.lipsanen.tasks.comments

import fi.metatavu.lipsanen.api.model.TaskComment
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Task comment translator
 */
@ApplicationScoped
class TaskCommentTranslator : AbstractTranslator<TaskCommentEntity, TaskComment>() {

    @Inject
    lateinit var taskCommentUserRepository: TaskCommentUserRepository

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override suspend fun translate(entity: TaskCommentEntity): TaskComment {
        return TaskComment(
            id = entity.id,
            comment = entity.comment,
            referencedUsers = taskCommentUserRepository.list(entity).map { it.user.id },
            taskId = entity.task.id,
            metadata = metadataTranslator.translate(entity),
        )
    }
}