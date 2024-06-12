package fi.metatavu.lipsanen.users

import fi.metatavu.lipsanen.persistence.AbstractRepository
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

@ApplicationScoped
class UserRepository: AbstractRepository<UserEntity, UUID>(){

    suspend fun create(
        id: UUID,
        keycloakId: UUID
    ): UserEntity {
        val userEntity = UserEntity()
        userEntity.id = id
        userEntity.keycloakId = keycloakId
        return persistSuspending(userEntity)
    }

    suspend fun findByKeycloakId(keycloakId: UUID): UserEntity? {
        return find("keycloakId", keycloakId).firstResult<UserEntity>().awaitSuspending()
    }
}