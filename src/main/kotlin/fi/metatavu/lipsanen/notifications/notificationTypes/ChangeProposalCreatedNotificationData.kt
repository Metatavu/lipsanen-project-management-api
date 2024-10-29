package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.proposals.ChangeProposalEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * Entity for storing change proposal created notifications
 */
@Table(name = "changeproposalcreatednotification")
@Entity
class ChangeProposalCreatedNotificationData : NotificationEntity() {

    @ManyToOne
    var changeProposal: ChangeProposalEntity? = null

    @Transient
    override var notificationType = NotificationType.CHANGE_PROPOSAL_CREATED

}