package fi.metatavu.lipsanen.tasks.comments

import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.users.UserEntity
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Task comment user repository
 */
@ApplicationScoped
class TaskCommentUserRepository : AbstractRepository<TaskCommentUser, UUID>() {

    /**
     * Lists task comment users
     *
     * @param taskCommentEntity task comment entity
     * @return list of task comment users
     */
    suspend fun list(taskCommentEntity: TaskCommentEntity): List<TaskCommentUser> {
        return list("taskComment", taskCommentEntity).awaitSuspending()
    }

    /**
     * Creates task comment to user connection
     *
     * @param randomUUID random UUID
     * @param createdComment created comment
     * @param user user
     * @return created task comment user
     */
    suspend fun create(randomUUID: UUID, createdComment: TaskCommentEntity, user: UserEntity): TaskCommentUser {
        val taskCommentUser = TaskCommentUser()
        taskCommentUser.id = randomUUID
        taskCommentUser.taskComment = createdComment
        taskCommentUser.user = user
        return persistSuspending(taskCommentUser)
    }
}