package fi.metatavu.lipsanen.tasks

import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.api.model.UserRole
import fi.metatavu.lipsanen.milestones.MilestoneEntity
import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.positions.JobPositionEntity
import fi.metatavu.lipsanen.users.UserEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

/**
 * Entity for tasks
 */
@Table(name = "task")
@Entity
class TaskEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var milestone: MilestoneEntity

    @ManyToOne
    var jobPosition: JobPositionEntity? = null

    @ManyToOne
    var dependentUser: UserEntity? = null

    @Column(nullable = false)
    lateinit var name: String

    @Column(nullable = false)
    lateinit var startDate: LocalDate

    @Column(nullable = false)
    lateinit var endDate: LocalDate

    @Enumerated(EnumType.STRING)
    lateinit var status: TaskStatus

    @Enumerated(EnumType.STRING)
    var userRole: UserRole? = null

    @Column(nullable = true)
    var estimatedDuration: Float? = null

    @Column(nullable = true)
    var estimatedReadiness: Int? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}