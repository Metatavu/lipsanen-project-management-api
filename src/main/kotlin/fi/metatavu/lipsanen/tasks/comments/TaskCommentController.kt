package fi.metatavu.lipsanen.tasks.comments

import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.api.model.TaskComment
import fi.metatavu.lipsanen.exceptions.UserNotFoundException
import fi.metatavu.lipsanen.notifications.NotificationsController
import fi.metatavu.lipsanen.tasks.TaskAssigneeRepository
import fi.metatavu.lipsanen.tasks.TaskController
import fi.metatavu.lipsanen.tasks.TaskEntity
import fi.metatavu.lipsanen.users.UserController
import fi.metatavu.lipsanen.users.UserEntity
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for Task Comments
 */
@ApplicationScoped
class TaskCommentController {

    @Inject
    lateinit var taskCommentRepository: TaskCommentRepository

    @Inject
    lateinit var commentUserRepository: TaskCommentUserRepository

    @Inject
    lateinit var notificationsController: NotificationsController

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var taskAssigneeRepository: TaskAssigneeRepository

    @Inject
    lateinit var userController: UserController

    /**
     * Lists task comments
     *
     * @param task task
     * @param first first result
     * @param max max results
     * @return pair of list of task comments and total count
     */
    suspend fun listTaskComments(
        task: TaskEntity,
        first: Int? = null,
        max: Int? = null
    ): Pair<List<TaskCommentEntity>, Long> {
        return taskCommentRepository.applyFirstMaxToQuery(
            taskCommentRepository.find("task", Sort.descending("createdAt"), task),
            first,
            max
        )
    }

    /**
     * Creates task comment
     *
     * @param task task
     * @param taskComment task comment
     * @param userId user id
     * @return created task comment
     * @throws UserNotFoundException if user is not found
     */
    suspend fun createTaskComment(task: TaskEntity, taskComment: TaskComment, userId: UUID): TaskCommentEntity {
        val createdComment = taskCommentRepository.create(
            id = UUID.randomUUID(),
            task = task,
            comment = taskComment.comment,
            creatorId = userId,
        )
        val notificationReceivers = mutableListOf<UserEntity>()
        taskComment.referencedUsers.forEach { refUser ->
            val user = userController.findUser(refUser) ?: throw UserNotFoundException(refUser)
            notificationReceivers.add(user)
            commentUserRepository.create(UUID.randomUUID(), createdComment, user)
        }
        notifyTaskComments(task, createdComment, notificationReceivers, userId)
        return createdComment
    }

    /**
     * Finds task comment by id
     *
     * @param commentId task comment id
     * @return task comment
     */
    suspend fun find(commentId: UUID): TaskCommentEntity? {
        return taskCommentRepository.findByIdSuspending(commentId)
    }

    /**
     * Updates task comment
     *
     * @param existingEntity existing task comment
     * @param taskComment task comment
     * @param userId user id
     * @return updated task comment
     * @throws UserNotFoundException if user is not found
     */
    suspend fun updateTaskComment(
        existingEntity: TaskCommentEntity,
        taskComment: TaskComment,
        userId: UUID
    ): TaskCommentEntity {
        existingEntity.comment = taskComment.comment
        existingEntity.lastModifierId = userId

        val currentReferencedUsers = commentUserRepository.list(existingEntity)
        val newReferencedUsers = taskComment.referencedUsers
        if (currentReferencedUsers.map { it.user.id }.toSet() != newReferencedUsers.toSet()) {
            currentReferencedUsers.forEach { commentUserRepository.deleteSuspending(it) }

            val notificationReceivers = mutableListOf<UserEntity>()
            newReferencedUsers.forEach { refUser ->
                val user = userController.findUser(refUser) ?: throw UserNotFoundException(refUser)
                notificationReceivers.add(user)
                commentUserRepository.create(UUID.randomUUID(), existingEntity, user)
            }
            notifyTaskComments(existingEntity.task, existingEntity, notificationReceivers, userId)
        }

        return taskCommentRepository.persistSuspending(existingEntity)
    }

    /**
     * Deletes task comment
     *
     * @param comment task comment
     */
    suspend fun deleteTaskComment(comment: TaskCommentEntity) {
        notificationsController.list(comment = comment).forEach { notificationsController.delete(it) }
        commentUserRepository.list(comment).forEach { commentUserRepository.deleteSuspending(it) }
        taskCommentRepository.deleteSuspending(comment)
    }

    /**
     * Creates notification for task comment, notifies mentioned users and task assignees but ignores the user that left the comment
     *
     * @param task task
     * @param mentionedUsers mentioned users
     * @param creatorId creator id
     */
    private suspend fun notifyTaskComments(task: TaskEntity, comment: TaskCommentEntity, mentionedUsers: List<UserEntity>, creatorId: UUID) {
        val taskAssignees = taskAssigneeRepository.listByTask(task).map { it.user }
        val commentCreator = userController.findUser(creatorId)

        notificationsController.createAndNotify(
            type = NotificationType.COMMENT_LEFT,
            taskEntity = task,
            comment = comment,
            receivers = mentionedUsers.plus(taskAssignees).minus(commentCreator).filterNotNull().distinctBy { it.id },
            creatorId = creatorId
        )
    }
}