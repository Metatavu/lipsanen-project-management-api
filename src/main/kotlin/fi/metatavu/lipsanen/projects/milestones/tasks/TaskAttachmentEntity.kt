package fi.metatavu.lipsanen.projects.milestones.tasks

import jakarta.persistence.*
import java.util.*

/**
 * Entity for task assignees
 */
@Entity
@Table(name = "task_attachment")
class TaskAttachmentEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var task: TaskEntity

    @Column(nullable = false)
    lateinit var attachmentUrl: String
}
