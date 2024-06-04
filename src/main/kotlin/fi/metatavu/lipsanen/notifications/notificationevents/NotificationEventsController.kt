package fi.metatavu.lipsanen.notifications.notificationevents

import fi.metatavu.lipsanen.api.model.NotificationEvent
import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for notification events
 */
@ApplicationScoped
class NotificationEventsController {

    @Inject
    lateinit var notificationEventRepository: NotificationEventRepository

    /**
     * Lists notification events
     *
     * @param userId user id
     * @param project project
     * @param notification notification
     * @param readStatus read status
     * @param task task
     * @param first first result
     * @param max max results
     * @return pair of list of notification events and total count
     */
    suspend fun list(
        userId: UUID? = null,
        project: ProjectEntity? = null,
        notification: NotificationEntity? = null,
        readStatus: Boolean? = null,
        task: TaskEntity? = null,
        first: Int? = null,
        max: Int? = null
    ): Pair<List<NotificationEventEntity>, Long> {
        return notificationEventRepository.list(
            userId = userId,
            project = project,
            notification = notification,
            readStatus = readStatus,
            task = task,
            first = first,
            max = max
        )
    }

    /**
     * Creates notification event
     *
     * @param notification notification
     * @param receiverId receiver id
     * @param creatorId creator id
     * @return created notification event
     */
    suspend fun create(notification: NotificationEntity, receiverId: UUID, creatorId: UUID): NotificationEventEntity {
        return notificationEventRepository.create(
            id = UUID.randomUUID(),
            notification = notification,
            receiverId = receiverId,
            read = false,
            creatorId = creatorId,
            lastModifierId = creatorId
        )
    }

    /**
     * Finds notification event by id
     *
     * @param notificationEventId notification event id
     * @return found notification event or null if not found
     */
    suspend fun find(notificationEventId: UUID): NotificationEventEntity? {
        return notificationEventRepository.findByIdSuspending(notificationEventId)
    }

    /**
     * Updates notification event
     *
     * @param existingEntity existing entity
     * @param updateBody update body
     * @param modifierId modifier id
     * @return updated notification event
     */
    suspend fun updateNotificationStatus(
        existingEntity: NotificationEventEntity,
        updateBody: NotificationEvent,
        modifierId: UUID
    ): NotificationEventEntity {
        existingEntity.readStatus = true
        existingEntity.lastModifierId = modifierId
        return notificationEventRepository.persistSuspending(existingEntity)
    }

    /**
     * Deletes notification event
     *
     * @param notificationEvent notification event
     */
    suspend fun delete(notificationEvent: NotificationEventEntity) {
        notificationEventRepository.deleteSuspending(notificationEvent)
    }
}