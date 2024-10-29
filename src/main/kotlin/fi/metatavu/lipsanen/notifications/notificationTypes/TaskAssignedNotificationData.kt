package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.notifications.NotificationEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * Entity for storing task assigned notifications
 */
@Table(name = "taskassignednotification")
@Entity
class TaskAssignedNotificationData: NotificationEntity() {

    @Column(nullable = false)
    lateinit var assigneeIds: String

    @Transient
    override var notificationType = NotificationType.TASK_ASSIGNED

}