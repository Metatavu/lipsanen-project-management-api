package fi.metatavu.lipsanen.users.userstoprojects

import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.users.UserEntity
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.*

/**
 * Entity for user to project connections
 */
@Entity
@Table(name = "usertoproject")
class UserToProjectEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var user: UserEntity

    @ManyToOne
    lateinit var project: ProjectEntity
}