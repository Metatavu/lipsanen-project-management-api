package fi.metatavu.lipsanen.projects.milestones.tasks.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposalStatus
import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

/**
 * Entity for change proposals
 */
@Entity
@Table(name = "changeproposal")
class ChangeProposalEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var task: TaskEntity

    @Column
    var reason: String? = null

    @Column
    var comment: String? = null

    @Enumerated(EnumType.STRING)
    lateinit var status: ChangeProposalStatus

    // Proposed changes

    @Column
    var startDate: LocalDate? = null

    @Column
    var endDate: LocalDate? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}