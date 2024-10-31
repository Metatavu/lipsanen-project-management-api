package fi.metatavu.lipsanen.projects.themes

import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.projects.ProjectEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotEmpty
import java.util.*

/**
 * Project theme entity
 */
@Table(name = "projecttheme")
@Entity
class ProjectThemeEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    @NotEmpty
    lateinit var themeColor: String

    @Column(nullable = false)
    @NotEmpty
    lateinit var logoUrl: String

    @ManyToOne
    lateinit var project: ProjectEntity

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}