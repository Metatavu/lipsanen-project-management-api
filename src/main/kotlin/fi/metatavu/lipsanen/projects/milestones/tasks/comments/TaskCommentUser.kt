package fi.metatavu.lipsanen.projects.milestones.tasks.comments

import jakarta.persistence.*
import java.util.*

/**
 * Entity class for task comment user
 */
@Entity
@Table(name = "taskcommentuser")
class TaskCommentUser {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var taskComment: TaskCommentEntity

    @Column(nullable = false)
    lateinit var userId: UUID
}