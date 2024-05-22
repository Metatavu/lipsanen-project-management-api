package fi.metatavu.lipsanen.projects.milestones.tasks

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "task_assignee")
class TaskAssigneeEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var task: TaskEntity

    @Column(nullable = false)
    lateinit var assigneeId: UUID
}
