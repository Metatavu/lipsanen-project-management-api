package fi.metatavu.lipsanen.projects

import fi.metatavu.lipsanen.persistence.Metadata
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotEmpty
import java.util.*

/**
 * Entity for projects
 */
@Table(name = "project")
@Entity
class ProjectEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    @NotEmpty
    lateinit var name: String

    @Column
    var tocomanId: Int? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}