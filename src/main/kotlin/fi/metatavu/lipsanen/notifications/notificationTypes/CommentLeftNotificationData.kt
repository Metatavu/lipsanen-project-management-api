package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.notifications.NotificationEntity
import fi.metatavu.lipsanen.tasks.comments.TaskCommentEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * Entity for storing comment left notifications
 */
@Table(name = "commentleftnotification")
@Entity
class CommentLeftNotificationData: NotificationEntity() {

    @ManyToOne
    lateinit var comment: TaskCommentEntity

    @Column(nullable = false)
    lateinit var commentText: String

    @Transient
    override var notificationType = NotificationType.COMMENT_LEFT
}