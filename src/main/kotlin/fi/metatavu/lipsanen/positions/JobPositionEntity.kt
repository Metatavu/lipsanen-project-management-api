package fi.metatavu.lipsanen.positions

import fi.metatavu.lipsanen.persistence.Metadata
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "job_position")
class JobPositionEntity: Metadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var name: String

    @Column
    var color: String? = null

    @Column
    var iconName: String? = null

    override lateinit var creatorId: UUID

    override lateinit var lastModifierId: UUID
}