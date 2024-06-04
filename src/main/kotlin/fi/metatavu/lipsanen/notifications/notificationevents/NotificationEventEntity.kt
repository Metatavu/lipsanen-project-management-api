package fi.metatavu.lipsanen.notifications.notificationevents

import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.persistence.Metadata
import jakarta.persistence.*
import java.util.*

/**
 * Entity for notification event
 */
@Table(name = "notificationevent")
@Entity
class NotificationEventEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var notification: NotificationEntity

    @Column(nullable = false)
    lateinit var receiverId: UUID

    @Column(nullable = false)
    var readStatus: Boolean? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}