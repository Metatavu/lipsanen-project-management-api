package fi.metatavu.lipsanen.projects

import fi.metatavu.lipsanen.api.model.ProjectStatus
import fi.metatavu.lipsanen.persistence.Metadata
import jakarta.persistence.*
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate
import java.util.*

/**
 * Entity for projects
 */
@Table(name = "project")
@Entity
class ProjectEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false, unique = true)
    @NotEmpty
    lateinit var name: String

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var status: ProjectStatus

    @Column
    var tocomanId: Int? = null

    @Column
    var estimatedStartDate: LocalDate? = null

    @Column
    var estimatedEndDate: LocalDate? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}