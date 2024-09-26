package fi.metatavu.lipsanen.notifications.notificationevents

import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.tasks.TaskEntity
import fi.metatavu.lipsanen.tasks.comments.TaskCommentEntity
import fi.metatavu.lipsanen.users.UserEntity
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for notification events
 */
@ApplicationScoped
class NotificationEventRepository : AbstractRepository<NotificationEventEntity, UUID>() {

    /**
     * Creates notification event
     *
     * @param id id
     * @param notification notification
     * @param receiver receiver
     * @param read read status
     * @param creatorId creator id
     * @param lastModifierId last modifier id
     * @return created notification event
     */
    suspend fun create(
        id: UUID,
        notification: NotificationEntity,
        receiver: UserEntity,
        read: Boolean,
        creatorId: UUID,
        lastModifierId: UUID
    ): NotificationEventEntity {
        val notificationEvent = NotificationEventEntity()
        notificationEvent.id = id
        notificationEvent.notification = notification
        notificationEvent.receiver = receiver
        notificationEvent.readStatus = read
        notificationEvent.creatorId = creatorId
        notificationEvent.lastModifierId = lastModifierId
        return persistSuspending(notificationEvent)
    }

    /**
     * Lists notification events
     *
     * @param userId user id
     * @param projectId project id
     * @param notification notification
     * @param first first result
     * @param max max results
     * @return pair of list of notification events and total count
     */
    suspend fun list(
        receiver: UserEntity?,
        project: ProjectEntity?,
        task: TaskEntity?,
        comment: TaskCommentEntity?,
        readStatus: Boolean?,
        notification: NotificationEntity?,
        first: Int?,
        max: Int?
    ): Pair<List<NotificationEventEntity>, Long> {
        val sb = StringBuilder()
        val parameters = Parameters()

        if (receiver != null) {
            addCondition(sb, ("receiver = :receiver"))
            parameters.and("receiver", receiver)
        }

        if (project != null) {
            addCondition(sb, ("notification.task.milestone.project = :project"))
            parameters.and("project", project)
        }

        if (task != null) {
            addCondition(sb, ("notification.task = :task"))
            parameters.and("task", task)
        }

        if (readStatus != null) {
            addCondition(sb, ("readStatus = :readStatus"))
            parameters.and("readStatus", readStatus)
        }

        if (notification != null) {
            addCondition(sb, ("notification = :notification"))
            parameters.and("notification", notification)
        }

        if (comment != null) {
            addCondition(sb, ("notification.comment = :comment"))
            parameters.and("comment", comment)
        }

        return applyFirstMaxToQuery(
            query = find(sb.toString(), Sort.descending("createdAt"), parameters),
            firstIndex = first,
            maxResults = max
        )
    }
}

