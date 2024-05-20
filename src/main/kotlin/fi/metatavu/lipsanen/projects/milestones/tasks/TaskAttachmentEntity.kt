package fi.metatavu.lipsanen.projects.milestones.tasks

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "task_attachment")
class TaskAttachmentEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    lateinit var taskId: UUID

    @Column(nullable = false)
    lateinit var attachmentUrl: String
}
