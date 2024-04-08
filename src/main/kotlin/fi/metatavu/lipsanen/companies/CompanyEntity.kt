package fi.metatavu.lipsanen.companies

import fi.metatavu.lipsanen.persistence.Metadata
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotEmpty
import java.util.*

/**
 * Company entity
 */
@Table(name = "company")
@Entity
class CompanyEntity : Metadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false, unique = true)
    @NotEmpty
    lateinit var name: String

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}