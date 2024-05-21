package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.api.model.UserRole
import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.projects.milestones.MilestoneEntity
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

    @Column(nullable = false)
    lateinit var name: String

    @Column(nullable = false)
    lateinit var startDate: LocalDate

    @Column(nullable = false)
    lateinit var endDate: LocalDate

    @Enumerated(EnumType.STRING)
    lateinit var status: TaskStatus

    @OneToMany(mappedBy = "id", cascade = [CascadeType.ALL], orphanRemoval = true, targetEntity = TaskAssigneeEntity::class)
    lateinit var assignees: List<TaskAssigneeEntity>

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var userRole: UserRole? = null

    @Column(nullable = true)
    var estimatedDuration: String? = null

    @Column(nullable = true)
    var estimatedReadiness: String? = null

    @OneToMany(mappedBy = "id", cascade = [CascadeType.ALL], orphanRemoval = true, targetEntity = TaskAttachmentEntity::class)
    lateinit var attachments: List<TaskAttachmentEntity>

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}