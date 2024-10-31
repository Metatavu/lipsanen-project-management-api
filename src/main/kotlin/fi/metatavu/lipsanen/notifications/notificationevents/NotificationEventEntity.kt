package fi.metatavu.lipsanen.notifications.notificationevents

import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.users.UserEntity
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

    @ManyToOne
    lateinit var receiver: UserEntity

    @Column(nullable = false)
    var readStatus: Boolean? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}