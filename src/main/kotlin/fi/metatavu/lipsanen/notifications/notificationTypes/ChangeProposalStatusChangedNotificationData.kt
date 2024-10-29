package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.api.model.ChangeProposalStatus
import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.proposals.ChangeProposalEntity
import jakarta.persistence.*
import kotlin.jvm.Transient

/**
 * Entity for storing change proposal status changed notifications
 */
@Table(name = "changeproposalstatuschangednotification")
@Entity
class ChangeProposalStatusChangedNotificationData: NotificationEntity() {

    @ManyToOne
    var changeProposal: ChangeProposalEntity? = null

    @Enumerated(EnumType.STRING)
    lateinit var status: ChangeProposalStatus

    @Transient
    override var notificationType = NotificationType.CHANGE_PROPOSAL_STATUS_CHANGED
}