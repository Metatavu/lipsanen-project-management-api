package fi.metatavu.lipsanen.users

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "user")
class UserEntity {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var keycloakId: UUID
}