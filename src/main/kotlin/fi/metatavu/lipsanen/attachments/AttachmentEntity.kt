package fi.metatavu.lipsanen.attachments

import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.tasks.TaskEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotEmpty
import java.util.*

/**
 * JPA entity representing an attachment
 */
@Table(name = "attachment")
@Entity
class AttachmentEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var url: String

    @ManyToOne
    lateinit var project: ProjectEntity

    @Column(nullable = false)
    @NotEmpty
    lateinit var attachmentType: String

    @Column(nullable = false)
    @NotEmpty
    lateinit var name: String

    @ManyToOne
    var task: TaskEntity? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}