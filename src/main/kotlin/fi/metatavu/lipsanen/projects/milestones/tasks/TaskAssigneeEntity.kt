package fi.metatavu.lipsanen.projects.milestones.tasks

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "task_assignee")
class TaskAssigneeEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    lateinit var taskId: UUID

    @Column(nullable = false)
    lateinit var assigneeId: String
}
