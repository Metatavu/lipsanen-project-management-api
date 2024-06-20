package fi.metatavu.lipsanen.users.userstoprojects

import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.users.UserEntity
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for user to project connections
 */
@ApplicationScoped
class UserToProjectRepository : AbstractRepository<UserToProjectEntity, UUID>() {

    /**
     * Creates user to project entity
     *
     * @param id id
     * @param user user
     * @param project project
     * @return created user to project entity
     */
    suspend fun create(
        id: UUID,
        user: UserEntity,
        project: ProjectEntity
    ): UserToProjectEntity {
        val userToProjectEntity = UserToProjectEntity()
        userToProjectEntity.id = id
        userToProjectEntity.user = user
        userToProjectEntity.project = project
        return persistSuspending(userToProjectEntity)
    }

    /**
     * Lists user to project entities
     *
     * @param user user
     * @return list of user to project entities
     */
    suspend fun list(user: UserEntity): List<UserToProjectEntity> {
        return list("user", user).awaitSuspending()
    }

    /**
     * Lists user to project entities
     *
     * @param project project
     * @return list of user to project entities
     */
    suspend fun list(project: ProjectEntity): List<UserToProjectEntity> {
        return list("project", project).awaitSuspending()
    }

    /**
     * Finds connection between user and project
     *
     * @param user user
     * @param project project
     * @return user to project entity
     */
    suspend fun list(user: UserEntity, project: ProjectEntity): UserToProjectEntity? {
        return find(
            "user = :user and project = :project",
            Parameters().and("user", user).and("project", project)
        ).firstResult<UserToProjectEntity>().awaitSuspending()
    }
}