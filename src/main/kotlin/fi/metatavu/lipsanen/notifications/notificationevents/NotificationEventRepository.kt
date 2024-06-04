package fi.metatavu.lipsanen.notifications.notificationevents

import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
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
     * @param receiverId receiver id
     * @param read read status
     * @param creatorId creator id
     * @param lastModifierId last modifier id
     * @return created notification event
     */
    suspend fun create(
        id: UUID,
        notification: NotificationEntity,
        receiverId: UUID,
        read: Boolean,
        creatorId: UUID,
        lastModifierId: UUID
    ): NotificationEventEntity {
        val notificationEvent = NotificationEventEntity()
        notificationEvent.id = id
        notificationEvent.notification = notification
        notificationEvent.receiverId = receiverId
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
        userId: UUID?,
        project: ProjectEntity?,
        task: TaskEntity?,
        readStatus: Boolean?,
        notification: NotificationEntity?,
        first: Int?,
        max: Int?
    ): Pair<List<NotificationEventEntity>, Long> {
        val sb = StringBuilder()
        val parameters = Parameters()

        if (userId != null) {
            addCondition(sb, ("receiverId = :receiverId"))
            parameters.and("receiverId", userId)
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

        return applyFirstMaxToQuery(
            query = find(sb.toString(), Sort.descending("createdAt"), parameters),
            firstIndex = first,
            maxResults = max
        )
    }
}

