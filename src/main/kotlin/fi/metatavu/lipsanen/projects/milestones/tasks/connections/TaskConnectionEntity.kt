package fi.metatavu.lipsanen.projects.milestones.tasks.connections

import fi.metatavu.lipsanen.api.model.TaskConnectionType
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import jakarta.persistence.*
import java.util.*

/**
 * Entity for task connections
 */
@Table(name = "taskconnection")
@Entity
class TaskConnectionEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var source: TaskEntity

    @ManyToOne
    lateinit var target: TaskEntity

    @Enumerated(EnumType.STRING)
    lateinit var type: TaskConnectionType
}