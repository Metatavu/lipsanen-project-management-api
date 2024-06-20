package fi.metatavu.lipsanen.projects.milestones.tasks.comments

import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import jakarta.persistence.*
import java.util.*

/**
 * Entity for Task Comments
 */
@Table(name = "taskcomment")
@Entity
class TaskCommentEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var task: TaskEntity

    @Column(nullable = false)
    lateinit var comment: String

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}
