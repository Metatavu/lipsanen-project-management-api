package fi.metatavu.lipsanen.milestones

import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.projects.ProjectEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate
import java.util.*

/**
 * Entity for milestones
 */
@Table(name = "milestone")
@Entity
class MilestoneEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var project: ProjectEntity

    @Column(nullable = false, unique = true)
    @NotEmpty
    lateinit var name: String

    @Column(nullable = false)
    lateinit var startDate: LocalDate

    @Column(nullable = false)
    lateinit var endDate: LocalDate

    @Column(nullable = false)
    lateinit var originalStartDate: LocalDate

    @Column(nullable = false)
    lateinit var originalEndDate: LocalDate

    @Column(nullable = true)
    var estimatedReadiness: Int? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}