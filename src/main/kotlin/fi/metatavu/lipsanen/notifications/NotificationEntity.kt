package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.proposals.ChangeProposalEntity
import fi.metatavu.lipsanen.tasks.TaskEntity
import fi.metatavu.lipsanen.tasks.comments.TaskCommentEntity
import jakarta.persistence.*
import java.util.*

/**
 * Entity class for notification
 */
@Table(name = "notification")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
class NotificationEntity: Metadata() {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var task: TaskEntity

    @Column(nullable = false)
    lateinit var taskName: String

    @Transient
    lateinit var notificationType: NotificationType

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}