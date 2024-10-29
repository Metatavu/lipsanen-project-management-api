package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.persistence.AbstractRepository
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.time.OffsetDateTime
import java.util.*

/**
 * Repository for notifications
 */
@ApplicationScoped
class NotificationRepository : AbstractRepository<NotificationEntity, UUID>() {

    /**
     * Lists notifications created after a certain date
     *
     * @param createdBeforeFilter created before filter
     * @return list of notifications
     */
    suspend fun listCreatedBefore(createdBeforeFilter: OffsetDateTime): List<NotificationEntity> {
        return list("createdAt <= ?1", createdBeforeFilter).awaitSuspending()
    }
}