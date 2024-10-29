package fi.metatavu.lipsanen.notifications.notificationTypes

import fi.metatavu.lipsanen.api.model.NotificationType
import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.notifications.NotificationEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * Entity for storing task status changed notifications
 */
@Table(name = "taskstatuschangednotification")
@Entity
class TaskStatusChangedNotificationData: NotificationEntity() {

    @Enumerated(EnumType.STRING)
    lateinit var status: TaskStatus

    @Transient
    override var notificationType = NotificationType.TASK_STATUS_CHANGED

}