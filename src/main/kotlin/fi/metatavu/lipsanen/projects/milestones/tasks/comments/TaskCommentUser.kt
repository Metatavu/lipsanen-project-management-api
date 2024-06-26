package fi.metatavu.lipsanen.projects.milestones.tasks.comments

import fi.metatavu.lipsanen.users.UserEntity
import jakarta.persistence.*
import java.util.*

/**
 * Entity class for users referenced in task comments
 */
@Entity
@Table(name = "taskcommentuser")
class TaskCommentUser {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var taskComment: TaskCommentEntity

    @ManyToOne
    lateinit var user: UserEntity
}