package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import jakarta.persistence.*
import java.util.*

/**
 * Entity class for notification
 */
@Table(name = "notification")
@Entity
class NotificationEntity: Metadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var message: String

    @Enumerated(EnumType.STRING)
    lateinit var type: NotificationType

    @ManyToOne
    var task: TaskEntity? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}
