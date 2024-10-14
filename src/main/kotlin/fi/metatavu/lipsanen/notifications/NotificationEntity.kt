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
class NotificationEntity: Metadata() {

    @Id
    lateinit var id: UUID

    @Enumerated(EnumType.STRING)
    lateinit var type: NotificationType

    @ManyToOne
    var task: TaskEntity? = null

    @ManyToOne
    var comment: TaskCommentEntity? = null

    @ManyToOne
    var changeProposal: ChangeProposalEntity? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}