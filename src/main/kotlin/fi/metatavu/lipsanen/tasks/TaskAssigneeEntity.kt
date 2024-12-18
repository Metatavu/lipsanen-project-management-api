package fi.metatavu.lipsanen.tasks

import fi.metatavu.lipsanen.users.UserEntity
import jakarta.persistence.*
import java.util.*

/**
 * Entity to connect tasks and assigned users
 */
@Entity
@Table(name = "task_assignee")
class TaskAssigneeEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var task: TaskEntity

    @ManyToOne
    lateinit var user: UserEntity
}
