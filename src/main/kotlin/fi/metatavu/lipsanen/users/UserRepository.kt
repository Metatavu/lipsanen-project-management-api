package fi.metatavu.lipsanen.users

import fi.metatavu.lipsanen.companies.CompanyEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for users
 */
@ApplicationScoped
class UserRepository: AbstractRepository<UserEntity, UUID>(){

    /**
     * Creates a new user
     *
     * @param id user id
     * @param keycloakId keycloak id
     * @param company company
     * @return created user
     */
    suspend fun create(
        id: UUID,
        keycloakId: UUID,
        company: CompanyEntity?
    ): UserEntity {
        val userEntity = UserEntity()
        userEntity.id = id
        userEntity.keycloakId = keycloakId
        userEntity.company = company
        return persistSuspending(userEntity)
    }

    /**
     * Finds user by keycloak id
     *
     * @param keycloakId keycloak id
     * @return user
     */
    suspend fun findByKeycloakId(keycloakId: UUID): UserEntity? {
        return find("keycloakId", keycloakId).firstResult<UserEntity>().awaitSuspending()
    }

    /**
     * Lists users
     *
     * @param companyEntity company entity
     * @param firstResult first result
     * @param maxResults max results
     * @return list of users
     */
    suspend fun list(companyEntity: CompanyEntity?, firstResult: Int?, maxResults: Int?): Pair<List<UserEntity>, Long> {
        val sb = StringBuilder()
        val parameters = Parameters()

        if (companyEntity != null) {
            sb.append("company = :company")
            parameters.and("company", companyEntity)
        }

        return applyFirstMaxToQuery(
            find(sb.toString(), parameters),
            firstResult,
            maxResults
        )
    }
}