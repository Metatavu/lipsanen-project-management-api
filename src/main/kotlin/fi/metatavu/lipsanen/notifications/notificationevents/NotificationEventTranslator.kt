package fi.metatavu.lipsanen.notifications.notificationevents

import fi.metatavu.lipsanen.api.model.NotificationEvent
import fi.metatavu.lipsanen.notifications.NotificationTranslator
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Translator for notification events
 */
@ApplicationScoped
class NotificationEventTranslator : AbstractTranslator<NotificationEventEntity, NotificationEvent>() {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    @Inject
    lateinit var notificationTranslator: NotificationTranslator

    override suspend fun translate(entity: NotificationEventEntity): NotificationEvent {
        return NotificationEvent(
            id = entity.id,
            notification = notificationTranslator.translate(entity.notification),
            receiverId = entity.receiver.id,
            read = entity.readStatus!!,
            metadata = metadataTranslator.translate(entity)
        )
    }

}