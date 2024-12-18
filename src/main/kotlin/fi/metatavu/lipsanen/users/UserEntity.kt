package fi.metatavu.lipsanen.users

import fi.metatavu.lipsanen.companies.CompanyEntity
import fi.metatavu.lipsanen.positions.JobPositionEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.*

/**
 * Entity for users
 */
@Entity
@Table(name = "user")
class UserEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne
    var company: CompanyEntity? = null

    @ManyToOne
    var jobPosition: JobPositionEntity? = null
}