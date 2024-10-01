package fi.metatavu.lipsanen.users

import fi.metatavu.lipsanen.companies.CompanyEntity
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.positions.JobPositionEntity
import fi.metatavu.lipsanen.projects.ProjectEntity
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
        company: CompanyEntity?,
        jobPosition: JobPositionEntity?
    ): UserEntity {
        val userEntity = UserEntity()
        userEntity.id = id
        userEntity.keycloakId = keycloakId
        userEntity.company = company
        userEntity.jobPosition = jobPosition
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
     * @param keycloakId keycloak id
     * @param jobPosition job position
     * @param project project
     * @param firstResult first result
     * @param maxResults max results
     * @return list of users
     */
    suspend fun list(
        companyEntity: CompanyEntity? = null,
        keycloakId: UUID?,
        jobPosition: JobPositionEntity? = null,
        project: List<ProjectEntity>? = null,
        firstResult: Int? = null,
        maxResults: Int? = null
    ): Pair<List<UserEntity>, Long> {
        val sb = StringBuilder("SELECT u FROM UserEntity u")
        val parameters = Parameters()

        if (project != null) {
            sb.append(" JOIN UserToProjectEntity utp ON u.id = utp.user.id")
            sb.append(" WHERE utp.project in :projects ")
            parameters.and("projects", project)
        }

        if (companyEntity != null) {
            addWhere(sb)
            sb.append(" u.company = :company")
            parameters.and("company", companyEntity)
        }

        if (jobPosition != null) {
            addWhere(sb)
            sb.append(" u.jobPosition = :jobPosition ")
            parameters.and("jobPosition", jobPosition)
        }

        if (keycloakId != null) {
            addWhere(sb)
            sb.append(" u.keycloakId = :keycloakId ")
            parameters.and("keycloakId", keycloakId)
        }

        return applyFirstMaxToQuery(
            find(sb.toString(), parameters),
            firstResult,
            maxResults
        )
    }

    /**
     * Adds where or end condition to HQL query
     *
     * @param sb string builder
     */
    private fun addWhere(sb: StringBuilder) {
        if (sb.contains("WHERE")) {
            sb.append(" AND")
        } else {
            sb.append(" WHERE")
        }
    }
}